package com.frietsync.backend.dto.user;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

public class ProfileUrlValidator implements ConstraintValidator<ProfileUrl, String> {

    private static final Pattern GITHUB_PATH = Pattern.compile("^/[A-Za-z0-9][A-Za-z0-9-]{0,38}/?$");
    private static final Pattern LINKEDIN_PATH = Pattern.compile("^/(?:in|company|pub)/[^/\\s]+(?:/.*)?$");

    private ProfileUrl.Type type;

    @Override
    public void initialize(ProfileUrl annotation) {
        this.type = annotation.type();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isValidUrl(type, value);
    }

    static boolean isValidUrl(ProfileUrl.Type type, String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
            return false;
        }
        if (uri.getRawUserInfo() != null || uri.getHost() == null) {
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        if (!host.contains(".")) {
            return false;
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        boolean defaultPort = uri.getPort() == -1;

        return switch (type) {
            case GITHUB -> defaultPort && host.equals("github.com")
                    && GITHUB_PATH.matcher(path).matches();
            case LINKEDIN -> defaultPort && isHostOrSubdomain(host, "linkedin.com")
                    && LINKEDIN_PATH.matcher(path).matches();
            case PORTFOLIO -> !isHostOrSubdomain(host, "github.com")
                    && !isHostOrSubdomain(host, "linkedin.com");
        };
    }

    private static boolean isHostOrSubdomain(String host, String domain) {
        return host.equals(domain) || host.endsWith("." + domain);
    }
}