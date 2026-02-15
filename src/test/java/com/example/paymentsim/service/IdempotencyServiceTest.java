package com.example.paymentsim.service;

import com.example.paymentsim.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class IdempotencyServiceTest {

  @Test
  void reuseKeyWithDifferentPayloadThrowsConflict() {
    StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);

    Mockito.when(redis.opsForValue()).thenReturn(ops);
    Mockito.when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
    Mockito.when(ops.get(anyString())).thenReturn("hashA|");

    IdempotencyService svc = new IdempotencyService(redis);

    assertThrows(ConflictException.class, () -> svc.claimOrValidate("k1", "hashB"));
  }
}
