
package com.frietsync.backend.service.impl.storage;

import com.frietsync.backend.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;
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

    private static final Set<String> CHAT_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "pdf", "txt", "md", "csv", "json", "log",
            "zip", "docx", "xlsx", "pptx", "mp3", "wav", "m4a", "ogg", "webm", "aac", "flac",
            "odt", "ods", "odp", "rtf", "7z", "tar", "gz"
    );

    private static final Set<String> PROFILE_PICTURE_EXTENSIONS =
            Set.of("png", "jpg", "jpeg", "webp");

    private static final Map<String, String> PROJECT_DOCUMENT_CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
            Map.entry("odp", "application/vnd.oasis.opendocument.presentation"),
            Map.entry("rtf", "application/rtf"),
            Map.entry("txt", "text/plain"),
            Map.entry("md", "text/markdown"),
            Map.entry("csv", "text/csv"),
            Map.entry("html", "text/html"),
            Map.entry("htm", "text/html"),
            Map.entry("xml", "application/xml"),
            Map.entry("epub", "application/epub+zip")
    );

    public static String validateIssueAttachment(
            MultipartFile file, String fileName) {
        return validate(fileName, ISSUE_EXTENSIONS);
    }

    public static String validateReleaseFile(
            MultipartFile file, String fileName) {
        return validate(fileName, RELEASE_EXTENSIONS);
    }

    public static String validateChatAttachment(MultipartFile file, String fileName) {
        return validate(fileName, CHAT_EXTENSIONS);
    }

    public static String validateProjectDocument(MultipartFile file, String fileName) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Document file must not be empty");
        }
        if (file.getSize() > 25L * 1024 * 1024) {
            throw new BadRequestException("Project documents cannot exceed 25 MB");
        }
        String extension = validate(fileName, PROJECT_DOCUMENT_CONTENT_TYPES.keySet());
        return PROJECT_DOCUMENT_CONTENT_TYPES.get(extension);
    }

    public static String validateProfilePicture(
            MultipartFile file, String fileName) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException(
                    "Profile picture must not be empty");
        }

        if (file.getSize() > 5L * 1024 * 1024) {
            throw new BadRequestException(
                    "Profile picture cannot exceed 5 MB");
        }

        String extension = validate(
                fileName, PROFILE_PICTURE_EXTENSIONS);

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw new BadRequestException(
                    "Unsupported profile picture type");
        };
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