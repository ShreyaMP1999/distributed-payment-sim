#!/usr/bin/env python3
"""Exercise the running HTTP app using only Python's standard library."""
import json
import os
import urllib.request
import uuid

origin = os.environ.get('APP_URL', 'http://localhost:8080').rstrip('/')
def request(path, body=None, key=None):
    headers = {'Content-Type': 'application/json'}
    if key:
        headers['Idempotency-Key'] = key
    data = None if body is None else json.dumps(body).encode()
    with urllib.request.urlopen(urllib.request.Request(origin+path, data=data, headers=headers), timeout=15) as response:
        return json.load(response)

assert request('/actuator/health')['status'] == 'UP'
a = request('/api/v1/accounts', {'ownerName': 'Smoke payer', 'initialBalanceCents': 1000})
b = request('/api/v1/accounts', {'ownerName': 'Smoke payee', 'initialBalanceCents': 0})
payload = {'payerAccountId': a['id'], 'payeeAccountId': b['id'], 'amountCents': 250}
key = str(uuid.uuid4())
payment = request('/api/v1/payments', payload, key)
assert payment['status'] == 'COMPLETED'
assert request('/api/v1/payments', payload, key) == payment
assert request('/api/v1/accounts/'+a['id'])['balanceCents'] == 750
assert request('/api/v1/accounts/'+b['id'])['balanceCents'] == 250
request('/api/v1/accounts/'+a['id']+'/deposit', {'amountCents': 50})
assert request('/api/v1/accounts/'+a['id'])['balanceCents'] == 800
with urllib.request.urlopen(origin+'/', timeout=15) as response:
    assert b'Payment Simulator' in response.read()
print('PASS: health, browser page, accounts, transfer, retry, balances, deposit')
