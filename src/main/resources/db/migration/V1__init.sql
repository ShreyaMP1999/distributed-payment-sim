CREATE TABLE accounts (
  id UUID PRIMARY KEY,
  owner_name VARCHAR(120) NOT NULL,
  balance_cents BIGINT NOT NULL,
  version BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_owner_name ON accounts(owner_name);

CREATE TABLE payments (
  id UUID PRIMARY KEY,
  idempotency_key VARCHAR(120) NOT NULL,
  payer_account_id UUID NOT NULL,
  payee_account_id UUID NOT NULL,
  amount_cents BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  request_hash VARCHAR(128) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_payments_idempotency_key ON payments(idempotency_key);
CREATE INDEX idx_payments_payer ON payments(payer_account_id);
CREATE INDEX idx_payments_payee ON payments(payee_account_id);

CREATE TABLE ledger_entries (
  id UUID PRIMARY KEY,
  account_id UUID NOT NULL,
  payment_id UUID NOT NULL,
  direction VARCHAR(10) NOT NULL,
  amount_cents BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_account ON ledger_entries(account_id);
CREATE INDEX idx_ledger_payment ON ledger_entries(payment_id);

ALTER TABLE payments
  ADD CONSTRAINT fk_payments_payer FOREIGN KEY (payer_account_id) REFERENCES accounts(id);

ALTER TABLE payments
  ADD CONSTRAINT fk_payments_payee FOREIGN KEY (payee_account_id) REFERENCES accounts(id);

ALTER TABLE ledger_entries
  ADD CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES accounts(id);

ALTER TABLE ledger_entries
  ADD CONSTRAINT fk_ledger_payment FOREIGN KEY (payment_id) REFERENCES payments(id);
