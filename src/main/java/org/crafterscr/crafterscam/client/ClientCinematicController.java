package org.crafterscr.crafterscam.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.crafterscr.crafterscam.network.NetCameraPoint;
import org.crafterscr.crafterscam.network.NetCameraSegment;

import java.util.List;

public class ClientCinematicController {
    private static boolean active = false;

    private static List<NetCameraSegment> segments = List.of();
    private static int segmentIndex = 0;
    private static int segmentTick = 0;

    private static ClientCinematicCameraEntity cameraEntity;
    private static float currentFov = 70.0F;

    public static void start(List<NetCameraSegment> incomingSegments) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null || incomingSegments == null || incomingSegments.isEmpty()) {
            return;
        }

        stop();

        segments = List.copyOf(incomingSegments);
        segmentIndex = 0;
        segmentTick = 0;
        active = true;

        cameraEntity = new ClientCinematicCameraEntity(minecraft.level);

        CameraFrame firstFrame = buildFrame(segments.get(0), 0);
        cameraEntity.applyFrame(firstFrame);
        currentFov = firstFrame.fov();

        minecraft.setCameraEntity(cameraEntity);
    }

    public static void stop() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null) {
            minecraft.setCameraEntity(minecraft.player);
        }

        active = false;
        segments = List.of();
        segmentIndex = 0;
        segmentTick = 0;
        cameraEntity = null;
        currentFov = 70.0F;
    }

    public static void tick() {
        if (!active) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null || segments.isEmpty()) {
            stop();
            return;
        }

        if (cameraEntity == null || cameraEntity.level() != minecraft.level) {
            cameraEntity = new ClientCinematicCameraEntity(minecraft.level);
        }

        if (minecraft.getCameraEntity() != cameraEntity) {
            minecraft.setCameraEntity(cameraEntity);
        }

        NetCameraSegment segment = segments.get(segmentIndex);
        CameraFrame frame = buildFrame(segment, segmentTick);

        cameraEntity.applyFrame(frame);
        currentFov = frame.fov();

        segmentTick++;

        if (segmentTick >= Math.max(1, segment.durationTicks())) {
            segmentIndex++;
            segmentTick = 0;

            if (segmentIndex >= segments.size()) {
                stop();
            }
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static float getCurrentFov() {
        return currentFov;
    }

    private static CameraFrame buildFrame(NetCameraSegment segment, int tick) {
        if (segment.type() == 0 || segment.type() == 2) {
            return fromPoint(segment.from());
        }

        float t;

        if (segment.durationTicks() <= 1) {
            t = 1.0F;
        } else {
            t = Mth.clamp(tick / (float) (segment.durationTicks() - 1), 0.0F, 1.0F);
        }

        t = applyEasing(segment.easing(), t);

        NetCameraPoint from = segment.from();
        NetCameraPoint to = segment.to();

        return new CameraFrame(
                Mth.lerp(t, from.x(), to.x()),
                Mth.lerp(t, from.y(), to.y()),
                Mth.lerp(t, from.z(), to.z()),
                Mth.rotLerp(t, from.yaw(), to.yaw()),
                Mth.lerp(t, from.pitch(), to.pitch()),
                Mth.lerp(t, from.roll(), to.roll()),
                Mth.lerp(t, from.fov(), to.fov())
        );
    }

    private static CameraFrame fromPoint(NetCameraPoint point) {
        return new CameraFrame(
                point.x(),
                point.y(),
                point.z(),
                point.yaw(),
                point.pitch(),
                point.roll(),
                point.fov()
        );
    }

    private static float applyEasing(int easing, float t) {
        if (easing == 1) {
            return t * t * (3.0F - 2.0F * t);
        }

        return t;
    }
}