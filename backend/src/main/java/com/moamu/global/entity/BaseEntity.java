package com.moamu.global.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

    @Column(name = "del_yn", columnDefinition = "CHAR(1)")
    private String delYn = "N";

    @CreatedDate
    @Column(name = "frst_reg_dt", updatable = false)
    private LocalDateTime frstRegDt;

    @LastModifiedDate
    @Column(name = "upd_dt")
    private LocalDateTime udtDt;
}
