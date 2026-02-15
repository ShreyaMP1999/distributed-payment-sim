package com.example.paymentsim.service;

import com.example.paymentsim.dto.AccountResponse;
import com.example.paymentsim.dto.CreateAccountRequest;
import com.example.paymentsim.dto.DepositRequest;
import com.example.paymentsim.entity.Account;
import com.example.paymentsim.exception.NotFoundException;
import com.example.paymentsim.repository.AccountRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService {

  private final AccountRepository accountRepository;

  public AccountService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Transactional
  public AccountResponse create(CreateAccountRequest req) {
    UUID id = UUID.randomUUID();
    Account account = new Account(id, req.ownerName(), req.initialBalanceCents());
    Account saved = accountRepository.save(account);
    return new AccountResponse(saved.getId(), saved.getOwnerName(), saved.getBalanceCents());
  }

  @Cacheable(value = "accounts", key = "#accountId.toString()")
  public AccountResponse get(UUID accountId) {
    Account a = accountRepository.findById(accountId)
        .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
    return new AccountResponse(a.getId(), a.getOwnerName(), a.getBalanceCents());
  }

  @Transactional
  public AccountResponse deposit(UUID accountId, DepositRequest req) {
    Account a = accountRepository.findById(accountId)
        .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));

    a.deposit(req.amountCents());
    Account saved = accountRepository.save(a);
    return new AccountResponse(saved.getId(), saved.getOwnerName(), saved.getBalanceCents());
  }
}
