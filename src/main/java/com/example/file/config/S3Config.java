package com.example.file.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * S3 클라이언트. prod 프로파일에서만 만들어짐.
 *
 * 자격 증명을 지정하지 않음 → 기본 자격 증명 체인.
 *   1. 환경 변수 AWS_ACCESS_KEY_ID ...   (이 과정에서는 쓰지 않음)
 *   2. ~/.aws/credentials               (이 과정에서는 쓰지 않음)
 *   3. 인스턴스 프로파일(EC2 역할)       (이 과정에서 쓰는 경로)
 *
 * 코드 · 설정 파일 · user-data 어디에도 키를 적지 않음.
 */
@Configuration
@Profile("prod")
public class S3Config {

    @Bean
    public S3Client s3Client(@Value("${app.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

}
