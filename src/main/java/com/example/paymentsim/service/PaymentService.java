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

  private final IdempotencyService idempotencyService;
  private final PaymentRepository paymentRepository;
  private final AccountRepository accountRepository;
  private final LedgerEntryRepository ledgerEntryRepository;

  public PaymentService(PaymentRepository paymentRepository,
                        AccountRepository accountRepository,
                        LedgerEntryRepository ledgerEntryRepository,
                        IdempotencyService idempotencyService) {
    this.idempotencyService = idempotencyService;
    this.paymentRepository = paymentRepository;
    this.accountRepository = accountRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
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

    // The committed database record is authoritative, even after Redis restarts.
    var existing = idempotencyService.findCompleted(idempotencyKey, requestHash);
    if (existing.isPresent()) {
      return toResponse(existing.get());
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
    paymentRepository.saveAndFlush(payment);

    // 4) Load accounts and apply business logic (optimistic locking via @Version on Account)
    Account payer = accountRepository.findById(req.payerAccountId())
        .orElseThrow(() -> new NotFoundException("Payer account not found: " + req.payerAccountId()));
    Account payee = accountRepository.findById(req.payeeAccountId())
        .orElseThrow(() -> new NotFoundException("Payee account not found: " + req.payeeAccountId()));

    // Validate both sides before changing managed entities.
    if (payer.getBalanceCents() < req.amountCents()) {
      throw new BadRequestException("Insufficient funds");
    }
    Math.addExact(payee.getBalanceCents(), req.amountCents());
    payer.withdraw(req.amountCents());
    payee.deposit(req.amountCents());
    accountRepository.save(payer);
    accountRepository.save(payee);
    ledgerEntryRepository.save(new LedgerEntry(UUID.randomUUID(), payer.getId(), paymentId, LedgerEntry.Direction.DEBIT, req.amountCents()));
    ledgerEntryRepository.save(new LedgerEntry(UUID.randomUUID(), payee.getId(), paymentId, LedgerEntry.Direction.CREDIT, req.amountCents()));
    payment.markCompleted();
    paymentRepository.save(payment);
    return toResponse(payment);
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

