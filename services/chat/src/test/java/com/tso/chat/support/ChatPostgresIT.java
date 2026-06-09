package com.tso.chat.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class ChatPostgresIT {

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
          .withDatabaseName("chat_test")
          .withUsername("tso")
          .withPassword("tso");

  static {
    POSTGRES.start();
  }

  public static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
      TestPropertyValues.of(
              "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
              "spring.datasource.username=" + POSTGRES.getUsername(),
              "spring.datasource.password=" + POSTGRES.getPassword(),
              "spring.flyway.locations=classpath:db/migration,classpath:test-db/migration")
          .applyTo(ctx.getEnvironment());
    }
  }
}
