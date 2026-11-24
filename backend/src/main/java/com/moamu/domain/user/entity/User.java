package com.moamu.domain.user.entity;

import org.hibernate.annotations.DynamicUpdate;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@DynamicUpdate
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "login_id", length = 50)
  private String loginId;

  @Column(name = "password", length = 255)
  private String password;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private UserStatus status = UserStatus.ACTIVE;

  @Column(name = "withdrawn_at")
  private LocalDateTime withdrawnAt;

  @Column(name = "profile_media_id")
  private Long profileMediaId;

  public static User createLocal(
      String loginId,
      String encodePassword,
      String name) {
    User user = new User();
    user.loginId = loginId;
    user.password = encodePassword;
    user.name = name;
    return user;
  }

  public void changeName(String name) {
    this.name = name;
  }

  public void changeProfilePhoto(Long mediaId) {
    this.profileMediaId = mediaId;
  }

  public void changePassword(String encodedPassword) {
    this.password = encodedPassword;
  }

  public void withdraw() {
    this.status = UserStatus.WITHDRAWN;
    this.withdrawnAt = LocalDateTime.now();
    this.loginId = null;
    this.password = null;
  }
}
