package com.moamu.domain.media.service;

import com.moamu.domain.media.exception.MediaErrorCode;
import com.moamu.global.exception.BusinessException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import openize.heic.decoder.HeicImage;
import openize.heic.decoder.PixelFormat;
import openize.io.IOFileStream;
import openize.io.IOMode;

// HEIC/HEIF 정지 이미지를 픽셀로 읽습니다. JPEG 인코딩은 최종 저장 단계에서만 합니다.
final class HeifConverter {

    // HEIC/HEIF 데이터를 디코더가 읽을 수 있도록 임시 파일로 저장
    BufferedImage read(byte[] original) {
        Path temp = null;
        try {
            temp = Files.createTempFile("moamu-heif-", ".heic");
            Files.write(temp, original);
            try (var input = new IOFileStream(temp, IOMode.READ)) {
                return decode(input);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("HEIC 이미지 변환 중 파일 처리에 실패했습니다.", e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    throw new UncheckedIOException("HEIC 임시 파일 정리에 실패했습니다.", e);
                }
            }
        }
    }

    // HEIC/HEIF 회전·반전이 적용된 이미지 객체를 반환합니다. 호출자가 사용 후 해제합니다.
    private BufferedImage decode(IOFileStream input) {
        BufferedImage argb = null;
        try {
            HeicImage image = HeicImage.load(input);
            checkSize(image.getWidth(), image.getHeight());
            for (var frame : image.getAllFrames().values()) {
                if (frame.isImage()) {
                    checkSize(frame.getWidth(), frame.getHeight());
                    byte[] bits = frame.getBitsPerChannel();
                    if (bits != null) {
                        for (byte bit : bits) {
                            if (Byte.toUnsignedInt(bit) > 8) {
                                throw new BusinessException(MediaErrorCode.UNSUPPORTED_IMAGE_FORMAT);
                            }
                        }
                    }
                }
            }
            int width = Math.toIntExact(image.getWidth());
            int height = Math.toIntExact(image.getHeight());
            int[] pixels = image.getInt32Array(PixelFormat.Argb32);
            if (pixels == null || pixels.length != (long) width * height) {
                throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
            }
            argb = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            argb.setRGB(0, 0, width, height, pixels, 0, width);
            return argb;
        } catch (BusinessException e) {
            if (argb != null) argb.flush();
            throw e;
        } catch (RuntimeException e) {
            if (argb != null) argb.flush();
            throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
        }
    }

    // 가로, 세로 유효한지 확인
    private void checkSize(long width, long height) {
        if (width <= 0 || height <= 0) throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
    }

}
