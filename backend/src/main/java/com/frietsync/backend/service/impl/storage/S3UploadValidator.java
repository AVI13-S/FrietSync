package com.frietsync.backend.service.impl.storage;

import com.frietsync.backend.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public final class S3UploadValidator {

    private static final Set<String> ISSUE_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp",
            "pdf", "txt", "md", "csv", "json", "log",
            "zip", "docx", "xlsx", "pptx"
    );

    private static final Set<String> RELEASE_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp",
            "pdf", "txt", "md", "csv", "json", "log",
            "zip", "docx", "xlsx", "pptx",
            "jar", "war", "apk", "aab", "exe", "msi",
            "dmg", "deb", "rpm", "gz", "tgz", "tar",
            "7z", "appimage"
    );

    private S3UploadValidator() {
    }

    public static String validateIssueAttachment(
            MultipartFile file, String fileName) {
        return validate(fileName, ISSUE_EXTENSIONS);
    }

    public static String validateReleaseFile(
            MultipartFile file, String fileName) {
        return validate(fileName, RELEASE_EXTENSIONS);
    }

    private static String validate(
            String fileName, Set<String> allowedExtensions) {

        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("File name is required");
        }

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new BadRequestException("Unsupported file type");
        }

        String extension = fileName.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);

        if (!allowedExtensions.contains(extension)) {
            throw new BadRequestException("Unsupported file type");
        }

        return extension;
    }
}