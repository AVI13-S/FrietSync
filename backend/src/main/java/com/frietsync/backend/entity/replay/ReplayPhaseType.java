package com.frietsync.backend.entity.replay;

public enum ReplayPhaseType {
    CREATED("Project created"),
    PLANNED("Planning"),
    DELAYED("Delays"),
    ISSUES("Issues raised"),
    RESOLVED("Issues resolved"),
    COMPLETED("Project completed");

    private final String defaultTitle;

    ReplayPhaseType(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }
}