package com.frietsync.backend.service.impl.user;

import com.frietsync.backend.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ProfilePictureUrlService {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket:}")
    private String bucketName;

    public String getUrl(User user) {
        if (user.getProfilePictureKey() == null || bucketName == null || bucketName.isBlank()) {
            return null;
        }
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(user.getProfilePictureKey())
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
