package com.moamu.domain.media.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class ImagePreprocessorTests {
    private final ImagePreprocessor processor = new ImagePreprocessor(1.0f);

    private byte[] encode(BufferedImage image, String format) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, format, bytes);
        return bytes.toByteArray();
    }

    private BufferedImage process(byte[] bytes, String extension) throws Exception {
        var validated = new ImageValidator().validate(
            new MockMultipartFile("image", "photo." + extension, "image/" + extension, bytes));
        return ImageIO.read(new ByteArrayInputStream(processor.process(validated)));
    }

    @ParameterizedTest
    @CsvSource({"3200,2400,1600,1200", "2400,3200,1200,1600", "300,200,300,200", "1600,1600,1600,1600"})
    void preservesRatioWithoutUpscaling(int width, int height, int expectedWidth, int expectedHeight) throws Exception {
        var result = process(encode(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpeg"), "jpg");
        assertEquals(expectedWidth, result.getWidth());
        assertEquals(expectedHeight, result.getHeight());
    }

    @Test
    void acceptsOverTwentyMillionPixelsWhenFileIsWithinTenMiB() throws Exception {
        var source = new BufferedImage(5000, 4001, BufferedImage.TYPE_INT_RGB);
        byte[] jpeg;
        try {
            jpeg = encode(source, "jpeg");
        } finally {
            source.flush();
        }
        assertTrue(jpeg.length <= 10 * 1024 * 1024);
        var result = process(jpeg, "jpg");
        assertEquals(1600, result.getWidth());
        assertEquals(1280, result.getHeight());
        result.flush();
    }

    @Test
    void preservesWebpWithoutUpscaling() throws Exception {
        byte[] webp = java.util.Base64.getDecoder().decode(
            "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
        var result = process(webp, "webp");
        assertEquals(1, result.getWidth());
        assertEquals(1, result.getHeight());
        assertFalse(result.getColorModel().hasAlpha());
    }

    @ParameterizedTest
    @ValueSource(strings = {"png", "webp"})
    void preservesFormatAndTransparencyWhenResizing(String format) throws Exception {
        var source = new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_ARGB);
        var graphics = source.createGraphics();
        graphics.setColor(new Color(255, 0, 0, 128));
        graphics.fillRect(100, 100, 200, 200);
        graphics.dispose();
        var validated = new ImageValidator().validate(new MockMultipartFile(
            "image", "photo." + format, "image/" + format, encode(source, format)));
        byte[] processed = processor.process(validated);
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(processed))) {
            var reader = ImageIO.getImageReaders(stream).next();
            try {
                assertEquals(format, reader.getFormatName().toLowerCase(java.util.Locale.ROOT));
            } finally {
                reader.dispose();
            }
        }
        var result = ImageIO.read(new ByteArrayInputStream(processed));
        assertEquals(1600, result.getWidth());
        assertEquals(800, result.getHeight());
        assertTrue(result.getColorModel().hasAlpha());
        assertEquals(0, result.getRGB(5, 5) >>> 24);
        assertEquals(128, result.getRGB(120, 120) >>> 24);
    }

    @ParameterizedTest
    @CsvSource({"1,0", "2,1", "3,3", "4,2", "5,0", "6,2", "7,3", "8,1"})
    void appliesAllExifOrientations(int orientation, int expectedTopLeft) throws Exception {
        Color[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
        var source = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        var g = source.createGraphics();
        for (int i = 0; i < 4; i++) {
            g.setColor(colors[i]);
            g.fillRect(i % 2 * 40, i / 2 * 20, 40, 20);
        }
        g.dispose();
        byte[] jpeg = encode(source, "jpeg");
        // JPEG APP1: Exif header + little-endian TIFF orientation entry.
        var exif = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        exif.put(new byte[]{'E','x','i','f',0,0,'I','I'}).putShort((short) 42).putInt(8);
        exif.putShort((short) 1).putShort((short) 0x112).putShort((short) 3).putInt(1);
        exif.putShort((short) orientation).putShort((short) 0).putInt(0);
        var bytes = new ByteArrayOutputStream();
        bytes.write(jpeg, 0, 2);
        bytes.write(new byte[]{(byte) 0xff, (byte) 0xe1, 0, 34});
        bytes.write(exif.array());
        bytes.write(jpeg, 2, jpeg.length - 2);
        var result = process(bytes.toByteArray(), "jpg");
        assertEquals(orientation >= 5 ? 40 : 80, result.getWidth());
        assertEquals(orientation >= 5 ? 80 : 40, result.getHeight());
        var actual = new Color(result.getRGB(8, 8));
        var expected = colors[expectedTopLeft];
        assertTrue(Math.abs(actual.getRed() - expected.getRed()) < 20);
        assertTrue(Math.abs(actual.getGreen() - expected.getGreen()) < 20);
        assertTrue(Math.abs(actual.getBlue() - expected.getBlue()) < 20);
    }

    @ParameterizedTest
    @ValueSource(floats = {-1, 1.1f, Float.NaN, Float.POSITIVE_INFINITY})
    void rejectsInvalidQuality(float quality) {
        assertThrows(IllegalArgumentException.class, () -> new ImagePreprocessor(quality));
    }
}
