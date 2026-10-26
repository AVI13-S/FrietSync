package com.frietsync.backend.config.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {
    @Bean
    public AwsCredentialsProvider s3CredentialsProvider(
            @Value("${aws.access-key-id:}") String accessKeyId,
            @Value("${aws.secret-access-key:}") String secretAccessKey,
            @Value("${aws.session-token:}") String sessionToken) {
        if (accessKeyId == null || accessKeyId.isBlank()
                || secretAccessKey == null || secretAccessKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        if (sessionToken != null && !sessionToken.isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken));
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }

    @Bean
    public S3Client s3Client(@Value("${aws.region:us-east-1}") String region,
                             AwsCredentialsProvider credentialsProvider) {
        return S3Client.builder().region(Region.of(region)).credentialsProvider(credentialsProvider).build();
    }

    @Bean
    public S3Presigner s3Presigner(@Value("${aws.region:us-east-1}") String region,
                                  AwsCredentialsProvider credentialsProvider) {
        return S3Presigner.builder().region(Region.of(region)).credentialsProvider(credentialsProvider).build();
    }
}
