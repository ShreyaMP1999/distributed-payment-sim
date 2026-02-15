package com.example.paymentsim.service;

import com.example.paymentsim.dto.CreatePaymentRequest;
import com.example.paymentsim.entity.Account;
import com.example.paymentsim.repository.AccountRepository;
import com.example.paymentsim.repository.LedgerEntryRepository;
import com.example.paymentsim.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class PaymentServiceTest {

  @Test
  void createPaymentCompletesAndReturnsResponse() {
    PaymentRepository paymentRepo = Mockito.mock(PaymentRepository.class);
    AccountRepository accountRepo = Mockito.mock(AccountRepository.class);
    LedgerEntryRepository ledgerRepo = Mockito.mock(LedgerEntryRepository.class);
    IdempotencyService idem = Mockito.mock(IdempotencyService.class);

    UUID payerId = UUID.randomUUID();
    UUID payeeId = UUID.randomUUID();

    Account payer = new Account(payerId, "payer", 10_000);
    Account payee = new Account(payeeId, "payee", 0);

    Mockito.when(accountRepo.findById(payerId)).thenReturn(Optional.of(payer));
    Mockito.when(accountRepo.findById(payeeId)).thenReturn(Optional.of(payee));
    Mockito.when(idem.getPaymentIdIfCompleted(any())).thenReturn(Optional.empty());

    Mockito.when(paymentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    Mockito.when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    Mockito.when(ledgerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PaymentService service = new PaymentService(paymentRepo, accountRepo, ledgerRepo, idem);

    var res = service.create("idem-1", new CreatePaymentRequest(payerId, payeeId, 5000));

    assertEquals(5000, res.amountCents());
    assertEquals(payerId, res.payerAccountId());
    assertEquals(payeeId, res.payeeAccountId());
    assertEquals(5000, payer.getBalanceCents());
    assertEquals(5000, payee.getBalanceCents());
  }
}
