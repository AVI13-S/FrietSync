package com.frietsync.backend.config.security;

import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.UUID;

public final class StompPrincipals {

    private StompPrincipals() {
    }
    public static UUID userId(Principal principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof Authentication) {
            Authentication authentication = (Authentication) principal;
            Object inner = authentication.getPrincipal();
            if (inner instanceof UUID) {
                return (UUID) inner;
            }
        }
        String name = principal.getName();
        if (name == null) {
            return null;
        }
        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}