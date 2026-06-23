package org.crafterscr.crafterscam.camera;

public enum CameraEasing {
    LINEAR(0),
    SMOOTH(1);

    private final int networkId;

    CameraEasing(int networkId) {
        this.networkId = networkId;
    }

    public int networkId() {
        return networkId;
    }

    public static CameraEasing safe(String name) {
        if (name == null) return SMOOTH;

        try {
            return CameraEasing.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return SMOOTH;
        }
    }
}