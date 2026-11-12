package com.moamu.global.entity;

import java.time.LocalDateTime;
import java.util.Optional;

import com.moamu.global.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.mapping.context.PersistentEntities;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityAuditingTests {

    @Test
    void creationLeavesModificationTimeEmpty() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 12, 10, 0);
        TestEntity entity = new TestEntity();
        AuditingHandler handler = auditingHandler();
        handler.setDateTimeProvider(() -> Optional.of(createdAt));

        handler.markCreated(entity);

        assertThat(entity.getDelYn()).isEqualTo("N");
        assertThat(entity.getFrstRegDt()).isEqualTo(createdAt);
        assertThat(entity.getUpdDt()).isNull();
    }

    @Test
    void modificationPreservesCreationTime() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 12, 10, 0);
        LocalDateTime modifiedAt = createdAt.plusHours(1);
        TestEntity entity = new TestEntity();
        AuditingHandler handler = auditingHandler();
        handler.setDateTimeProvider(() -> Optional.of(createdAt));
        handler.markCreated(entity);
        handler.setDateTimeProvider(() -> Optional.of(modifiedAt));

        handler.markModified(entity);

        assertThat(entity.getFrstRegDt()).isEqualTo(createdAt);
        assertThat(entity.getUpdDt()).isEqualTo(modifiedAt);
    }

    private AuditingHandler auditingHandler() {
        AuditingHandler handler = new AuditingHandler(PersistentEntities.of());
        handler.setModifyOnCreation(JpaConfig.class.getAnnotation(EnableJpaAuditing.class).modifyOnCreate());
        return handler;
    }

    private static class TestEntity extends BaseEntity {
    }
}
