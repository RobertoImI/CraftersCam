package org.crafterscr.crafterscam.camera;

import java.util.ArrayList;
import java.util.List;

public class CameraSequence {
    public String id = "";
    public List<CameraStep> steps = new ArrayList<>();

    public CameraSequence() {
    }

    public CameraSequence(String id) {
        this.id = id;
    }

    public String getLastPointId() {
        if (steps.isEmpty()) {
            return null;
        }

        CameraStep last = steps.get(steps.size() - 1);
        StepType type = StepType.safe(last.type);

        if (type == StepType.MOVE) {
            return last.to;
        }

        return last.from;
    }
}