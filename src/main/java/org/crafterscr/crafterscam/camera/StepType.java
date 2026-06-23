package org.crafterscr.crafterscam.camera;

public enum StepType {
    HOLD(0),
    MOVE(1),
    CUT(2);

    private final int networkId;

    StepType(int networkId) {
        this.networkId = networkId;
    }

    public int networkId() {
        return networkId;
    }

    public static StepType safe(String name) {
        if (name == null) return HOLD;

        try {
            return StepType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return HOLD;
        }
    }
}