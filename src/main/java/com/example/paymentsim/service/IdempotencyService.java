package com.example.paymentsim.service;

import com.example.paymentsim.exception.ConflictException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class IdempotencyService {

  private static final Duration TTL = Duration.ofHours(24);

  private final StringRedisTemplate redis;

  public IdempotencyService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  private String key(String idempotencyKey) {
    return "idem:" + idempotencyKey;
  }

  /**
   * Stores: requestHash|paymentId (paymentId may be empty while in-progress)
   */
  public void claimOrValidate(String idempotencyKey, String requestHash) {
    String redisKey = key(idempotencyKey);
    String value = requestHash + "|";
    Boolean ok = redis.opsForValue().setIfAbsent(redisKey, value, TTL);
    if (Boolean.TRUE.equals(ok)) return; // claimed

    String existing = redis.opsForValue().get(redisKey);
    if (existing == null) return;

    String existingHash = existing.split("\\|", 2)[0];
    if (!existingHash.equals(requestHash)) {
      throw new ConflictException("Idempotency-Key reuse with different request payload");
    }
  }

  public Optional<String> getPaymentIdIfCompleted(String idempotencyKey) {
    String existing = redis.opsForValue().get(key(idempotencyKey));
    if (existing == null) return Optional.empty();
    String[] parts = existing.split("\\|", 2);
    if (parts.length < 2) return Optional.empty();
    String paymentId = parts[1];
    if (paymentId == null || paymentId.isBlank()) return Optional.empty();
    return Optional.of(paymentId);
  }

  public void markCompleted(String idempotencyKey, String requestHash, String paymentId) {
    String redisKey = key(idempotencyKey);
    redis.opsForValue().set(redisKey, requestHash + "|" + paymentId, TTL);
  }
}
