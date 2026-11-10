ALTER TABLE accounts ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC';
ALTER TABLE accounts ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at AT TIME ZONE 'UTC';
ALTER TABLE payments ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC';
ALTER TABLE ledger_entries ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC';
ALTER TABLE accounts ADD CONSTRAINT positive_balance CHECK (balance_cents >= 0);
ALTER TABLE payments ADD CONSTRAINT positive_payment CHECK (amount_cents > 0);
ALTER TABLE payments ADD CONSTRAINT different_accounts CHECK (payer_account_id <> payee_account_id);
ALTER TABLE ledger_entries ADD CONSTRAINT positive_ledger_amount CHECK (amount_cents > 0);
