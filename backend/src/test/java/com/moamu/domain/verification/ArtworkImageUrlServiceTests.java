package com.moamu.domain.verification;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import com.moamu.domain.verification.service.ArtworkImageUrlService;
import static org.assertj.core.api.Assertions.*;

class ArtworkImageUrlServiceTests {
    @Test
    void signsArtworkKeyWithoutCallingS3() {
        try (var presigner = S3Presigner.builder().region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build()) {
            var beans = new StaticListableBeanFactory();
            beans.addBean("presigner", presigner);
            var service = new ArtworkImageUrlService(beans.getBeanProvider(S3Presigner.class),
                    new org.springframework.mock.env.MockEnvironment().withProperty("app.storage.s3.bucket", "moamu-test"));
            var url = URI.create(service.resolve("artworks/test image.png"));
            assertThat(url.getHost()).isEqualTo("moamu-test.s3.ap-northeast-2.amazonaws.com");
            assertThat(url.getPath()).isEqualTo("/artworks/test image.png");
            assertThat(url.getQuery()).contains("X-Amz-Expires=900", "X-Amz-Signature=");
            assertThat(service.resolve(null)).isNull();
        }
    }
}
