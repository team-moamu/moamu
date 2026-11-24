package com.moamu.global.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Sha256 {

    private Sha256() {
    }

    public static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
