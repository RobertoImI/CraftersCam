package org.crafterscr.crafterscam.camera;

public class CameraStep {
    public String type = StepType.HOLD.name();

    public String from = "";
    public String to = "";

    public int durationTicks = 20;
    public String easing = CameraEasing.SMOOTH.name();

    public CameraStep() {
    }

    public CameraStep(String type, String from, String to, int durationTicks, String easing) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.durationTicks = Math.max(1, durationTicks);
        this.easing = easing;
    }

    public static CameraStep hold(String pointId, int seconds) {
        return new CameraStep(
                StepType.HOLD.name(),
                pointId,
                pointId,
                Math.max(1, seconds * 20),
                CameraEasing.SMOOTH.name()
        );
    }

    public static CameraStep move(String fromPoint, String toPoint, int seconds, String easing) {
        return new CameraStep(
                StepType.MOVE.name(),
                fromPoint,
                toPoint,
                Math.max(1, seconds * 20),
                CameraEasing.safe(easing).name()
        );
    }

    public static CameraStep cut(String pointId) {
        return new CameraStep(
                StepType.CUT.name(),
                pointId,
                pointId,
                1,
                CameraEasing.LINEAR.name()
        );
    }
}