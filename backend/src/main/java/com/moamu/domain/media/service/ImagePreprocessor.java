package com.moamu.domain.media.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.moamu.domain.media.exception.MediaErrorCode;
import com.moamu.global.exception.BusinessException;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 이미지 방향과 크기 보정
@Component
public class ImagePreprocessor {
    private static final int MAX_EDGE = 1600;
    private final float jpegQuality;

    // JPEG 품질 설정을 받아 유한한 0~1 범위의 값인지 검증
    public ImagePreprocessor(@Value("${app.media.jpeg-quality:1.0}") float jpegQuality) {
        if (!Float.isFinite(jpegQuality) || jpegQuality < 0 || jpegQuality > 1) {
            throw new IllegalArgumentException("JPEG 품질은 0~1 사이여야 합니다.");
        }
        this.jpegQuality = jpegQuality;
    }

    // 이미지 방향을 보정 및 비율 유지
    // 긴 변 최대 1600px로 축소
    byte[] process(ImageValidator.ValidatedImage validated) {
        BufferedImage source = null;
        BufferedImage output = null;
        try {
            // HEIC/HEIF는 이미 디코딩한 픽셀을 직접 사용하여 중간 JPEG 인코딩/재디코딩을 생략
            source = validated.decodedImage() != null ? validated.decodedImage()
                : ImageIO.read(new ByteArrayInputStream(validated.bytes()));
            if (source == null) throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
            // HEIC/HEIF 회전,반전은 디코더에서 처리했으므로 EXIF 미적용
            int orientation = validated.decodedImage() != null ? 1 : orientation(validated.bytes());
            int width = source.getWidth();
            int height = source.getHeight();
            boolean swapped = orientation >= 5;
            int orientedWidth = swapped ? height : width;
            int orientedHeight = swapped ? width : height;
            double scale = Math.min(1.0, (double) MAX_EDGE / Math.max(width, height));
            int targetWidth = Math.max(1, (int) Math.round(orientedWidth * scale));
            int targetHeight = Math.max(1, (int) Math.round(orientedHeight * scale));
            boolean preserveAlpha = !validated.extension().equals("jpg") && source.getColorModel().hasAlpha();
            output = new BufferedImage(targetWidth, targetHeight,
                preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            // 원본 이미지 방향 보정 또는 축소하여 새 이미지 생성
            var graphics = output.createGraphics();
            try {
                // JPEG로 저장할 투명 이미지 흰색 배경으로 합성
                if (!preserveAlpha && source.getColorModel().hasAlpha()) {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, targetWidth, targetHeight);
                }
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.scale((double) targetWidth / orientedWidth, (double) targetHeight / orientedHeight);
                graphics.drawImage(source, transform(orientation, width, height), null);
            } finally {
                graphics.dispose();
            }
            return encode(output, validated.extension());
        } catch (IOException | ImageProcessingException e) {
            throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
        } finally {
            if (source != null && validated.decodedImage() == null) source.flush();
            if (output != null) output.flush();
        }
    }

    // EXIF 방향 값(1~8) 확인
    // 방향 값이 없거나 범위를 벗어나면 정상 방향(1)을 반환
    private int orientation(byte[] bytes) throws IOException, ImageProcessingException {
        var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
        var directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        Integer value = directory == null ? null : directory.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
        return value != null && value >= 1 && value <= 8 ? value : 1;
    }

    // EXIF 방향에 맞는 회전,반전 변환을 생성하고 이미지가 영역 안에 들어오도록 위치 보정
    static AffineTransform transform(int orientation, int width, int height) {
        return switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, width, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, height);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, height, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, height, width);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, width);
            default -> new AffineTransform();
        };
    }

    // 보정된 이미지를 지정한 형식의 파일 데이터(byte[])로 변환
    // JPEG 설정 품질을 적용
    // PNG, WebP 무손실 압축으로 저장
    private byte[] encode(BufferedImage image, String format) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) throw new IllegalStateException("이미지 인코더가 없습니다: " + format);
        var writer = writers.next();
        try (var bytes = new ByteArrayOutputStream(); var stream = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(stream);
            var params = writer.getDefaultWriteParam();
            if (format.equals("jpg")) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(jpegQuality);
            } else if (format.equals("webp")) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionType("Lossless");
            }
            // 회전 보정 후 EXIF를 복사하지 않아 이중 회전을 방지한다.
            writer.write(null, new IIOImage(image, null, null), params);
            stream.flush();
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
