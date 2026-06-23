package org.crafterscr.crafterscam.camera;

import java.util.List;

public enum CameraEasing {
    LINEAR(0),
    SMOOTH(1),
    EASE_IN(2),
    EASE_OUT(3),
    EASE_IN_OUT(4);

    private final int networkId;

    CameraEasing(int networkId) {
        this.networkId = networkId;
    }

    public int networkId() {
        return networkId;
    }

    public static CameraEasing safe(String name) {
        if (name == null || name.isBlank()) {
            return SMOOTH;
        }

        String normalized = name.trim()
                .toUpperCase()
                .replace("-", "_");

        if (normalized.equals("EASEIN")) {
            normalized = "EASE_IN";
        } else if (normalized.equals("EASEOUT")) {
            normalized = "EASE_OUT";
        } else if (normalized.equals("EASEINOUT")) {
            normalized = "EASE_IN_OUT";
        }

        try {
            return CameraEasing.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return SMOOTH;
        }
    }

    public static List<String> suggestions() {
        return List.of(
                "linear",
                "smooth",
                "ease_in",
                "ease_out",
                "ease_in_out"
        );
    }
}