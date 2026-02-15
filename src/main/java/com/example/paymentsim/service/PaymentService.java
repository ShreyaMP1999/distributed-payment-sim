package com.example.paymentsim.service;

import com.example.paymentsim.dto.CreatePaymentRequest;
import com.example.paymentsim.dto.PaymentResponse;
import com.example.paymentsim.entity.Account;
import com.example.paymentsim.entity.LedgerEntry;
import com.example.paymentsim.entity.Payment;
import com.example.paymentsim.exception.BadRequestException;
import com.example.paymentsim.exception.NotFoundException;
import com.example.paymentsim.repository.AccountRepository;
import com.example.paymentsim.repository.LedgerEntryRepository;
import com.example.paymentsim.repository.PaymentRepository;
import com.example.paymentsim.util.HashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final AccountRepository accountRepository;
  private final LedgerEntryRepository ledgerEntryRepository;
  private final IdempotencyService idempotencyService;

  public PaymentService(PaymentRepository paymentRepository,
                        AccountRepository accountRepository,
                        LedgerEntryRepository ledgerEntryRepository,
                        IdempotencyService idempotencyService) {
    this.paymentRepository = paymentRepository;
    this.accountRepository = accountRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.idempotencyService = idempotencyService;
  }

  public PaymentResponse get(UUID id) {
    Payment p = paymentRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Payment not found: " + id));
    return toResponse(p);
  }

  @Transactional
  public PaymentResponse create(String idempotencyKey, CreatePaymentRequest req) {
    if (req.payerAccountId().equals(req.payeeAccountId())) {
      throw new BadRequestException("payerAccountId and payeeAccountId must be different");
    }

    String requestHash = HashUtil.sha256(req.payerAccountId() + "|" + req.payeeAccountId() + "|" + req.amountCents());

    // 1) claim/validate idempotency in Redis
    idempotencyService.claimOrValidate(idempotencyKey, requestHash);

    // 2) If already completed, return existing payment
    var existingPaymentId = idempotencyService.getPaymentIdIfCompleted(idempotencyKey);
    if (existingPaymentId.isPresent()) {
      UUID pid = UUID.fromString(existingPaymentId.get());
      Payment existing = paymentRepository.findById(pid)
          .orElseThrow(() -> new NotFoundException("Idempotent payment missing in DB: " + pid));
      return toResponse(existing);
    }

    // 3) Create payment record (PENDING)
    UUID paymentId = UUID.randomUUID();
    Payment payment = new Payment(
        paymentId,
        idempotencyKey,
        req.payerAccountId(),
        req.payeeAccountId(),
        req.amountCents(),
        Payment.Status.PENDING,
        requestHash
    );
    paymentRepository.save(payment);

    // 4) Load accounts and apply business logic (optimistic locking via @Version on Account)
    Account payer = accountRepository.findById(req.payerAccountId())
        .orElseThrow(() -> new NotFoundException("Payer account not found: " + req.payerAccountId()));
    Account payee = accountRepository.findById(req.payeeAccountId())
        .orElseThrow(() -> new NotFoundException("Payee account not found: " + req.payeeAccountId()));

    try {
      payer.withdraw(req.amountCents());
      payee.deposit(req.amountCents());

      accountRepository.save(payer);
      accountRepository.save(payee);

      // 5) Ledger entries
      ledgerEntryRepository.save(new LedgerEntry(UUID.randomUUID(), payer.getId(), paymentId, LedgerEntry.Direction.DEBIT, req.amountCents()));
      ledgerEntryRepository.save(new LedgerEntry(UUID.randomUUID(), payee.getId(), paymentId, LedgerEntry.Direction.CREDIT, req.amountCents()));

      // 6) Mark completed
      payment.markCompleted();
      paymentRepository.save(payment);

      // 7) Mark idempotency mapping
      idempotencyService.markCompleted(idempotencyKey, requestHash, paymentId.toString());

      return toResponse(payment);
    } catch (RuntimeException ex) {
      payment.markFailed();
      paymentRepository.save(payment);
      throw ex;
    }
  }

  private PaymentResponse toResponse(Payment p) {
    return new PaymentResponse(
        p.getId(),
        p.getIdempotencyKey(),
        p.getPayerAccountId(),
        p.getPayeeAccountId(),
        p.getAmountCents(),
        p.getStatus()
    );
  }
}
