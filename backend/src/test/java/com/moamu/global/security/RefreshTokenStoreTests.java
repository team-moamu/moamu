package com.moamu.global.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RefreshTokenStoreTests {

  @Container
  static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.2"))
      .withExposedPorts(6379);

  static LettuceConnectionFactory connectionFactory;
  static RefreshTokenStore store;

  @BeforeAll
  static void setUp() {
    connectionFactory = new LettuceConnectionFactory(
        redis.getHost(),
        redis.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();

    StringRedisTemplate template = new StringRedisTemplate(connectionFactory);

    store = new RefreshTokenStore(template);
  }

  @AfterAll
  static void tearDown() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
  }

  @Test
  void savedTokenCanOnlyBeConsumedOnce() {
    String token = UUID.randomUUID().toString();

    store.save(token, 1L, Duration.ofMinutes(1));

    assertEquals(1L, store.consume(token).orElseThrow());
    assertTrue(store.consume(token).isEmpty());
  }

  @Test
  void revokedTokenCannotBeConsumed() {
    String token = UUID.randomUUID().toString();

    store.save(token, 1L, Duration.ofMinutes(1));
    store.revoke(token);

    assertTrue(store.consume(token).isEmpty());
  }

  @Test
  void expiredTokenCannotBeConsumed() throws InterruptedException {
    String token = UUID.randomUUID().toString();

    store.save(token, 1L, Duration.ofMillis(200));
    Thread.sleep(500);

    assertTrue(store.consume(token).isEmpty());
  }

  @Test
  void duplicateSaveDoesNotOverwriteOwner() {
    String token = UUID.randomUUID().toString();

    store.save(token, 1L, Duration.ofMinutes(1));

    assertThrows(
        IllegalStateException.class,
        () -> store.save(token, 2L, Duration.ofMinutes(1)));

    assertEquals(1L, store.consume(token).orElseThrow());
  }
}