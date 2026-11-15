package com.frietsync.backend.exception;

public class GitHubReauthorizationRequiredException extends RuntimeException {
    public GitHubReauthorizationRequiredException() {
        super("GitHub authorization has expired. Please reconnect your GitHub account.");
    }

    public GitHubReauthorizationRequiredException(Throwable cause) {
        super("GitHub authorization has expired. Please reconnect your GitHub account.", cause);
    }
}
