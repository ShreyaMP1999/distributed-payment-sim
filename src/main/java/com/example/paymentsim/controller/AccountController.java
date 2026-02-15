package com.example.paymentsim.controller;

import com.example.paymentsim.dto.*;
import com.example.paymentsim.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AccountResponse create(@Valid @RequestBody CreateAccountRequest req) {
    return accountService.create(req);
  }

  @GetMapping("/{id}")
  public AccountResponse get(@PathVariable UUID id) {
    return accountService.get(id);
  }

  @PostMapping("/{id}/deposit")
  public AccountResponse deposit(@PathVariable UUID id, @Valid @RequestBody DepositRequest req) {
    return accountService.deposit(id, req);
  }
}
