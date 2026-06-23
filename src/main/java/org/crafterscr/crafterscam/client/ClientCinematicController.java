package org.crafterscr.crafterscam.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.crafterscr.crafterscam.network.NetCameraPoint;
import org.crafterscr.crafterscam.network.NetCameraSegment;

import java.util.List;

public class ClientCinematicController {
    private static boolean active = false;

    private static List<NetCameraSegment> segments = List.of();
    private static int segmentIndex = 0;
    private static int segmentTick = 0;

    private static int elapsedTicks = 0;
    private static int totalTicks = 0;

    private static int fadeInTicks = 10;
    private static int fadeOutTicks = 10;
    private static boolean showBars = true;
    private static boolean hideHudAll = true;
    private static boolean allowMovement = true;

    private static ClientCinematicCameraEntity cameraEntity;
    private static float currentFov = 70.0F;

    public static void start(
            List<NetCameraSegment> incomingSegments,
            int incomingFadeInTicks,
            int incomingFadeOutTicks,
            boolean incomingShowBars,
            boolean incomingHideHudAll,
            boolean incomingAllowMovement
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null || incomingSegments == null || incomingSegments.isEmpty()) {
            return;
        }

        stop();

        segments = List.copyOf(incomingSegments);
        segmentIndex = 0;
        segmentTick = 0;
        elapsedTicks = 0;

        totalTicks = 0;
        for (NetCameraSegment segment : segments) {
            totalTicks += Math.max(1, segment.durationTicks());
        }

        fadeInTicks = Math.max(0, incomingFadeInTicks);
        fadeOutTicks = Math.max(0, incomingFadeOutTicks);
        showBars = incomingShowBars;
        hideHudAll = incomingHideHudAll;
        allowMovement = incomingAllowMovement;

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
        elapsedTicks = 0;
        totalTicks = 0;

        cameraEntity = null;
        currentFov = 70.0F;

        fadeInTicks = 10;
        fadeOutTicks = 10;
        showBars = true;
        hideHudAll = true;
        allowMovement = true;
    }

    public static void beforeClientTick() {
        if (active && !allowMovement) {
            blockHorizontalMovement();
        }
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

        if (!allowMovement) {
            blockHorizontalMovement();
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
        elapsedTicks++;

        if (segmentTick >= Math.max(1, segment.durationTicks())) {
            segmentIndex++;
            segmentTick = 0;

            if (segmentIndex >= segments.size()) {
                stop();
            }
        }
    }

    private static void blockHorizontalMovement() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null) {
            return;
        }

        if (player.input != null) {
            player.input.forwardImpulse = 0.0F;
            player.input.leftImpulse = 0.0F;

            player.input.up = false;
            player.input.down = false;
            player.input.left = false;
            player.input.right = false;

            // No tocamos player.input.jumping.
            // Así el jugador puede brincar, pero no caminar.
        }

        Vec3 velocity = player.getDeltaMovement();

        // Bloquea desplazamiento horizontal.
        // Mantiene Y para permitir salto/caída.
        player.setDeltaMovement(0.0D, velocity.y, 0.0D);
    }

    public static boolean isActive() {
        return active;
    }

    public static float getCurrentFov() {
        return currentFov;
    }

    public static boolean shouldShowBars() {
        return active && showBars;
    }

    public static boolean shouldHideHudAll() {
        return active && hideHudAll;
    }

    public static boolean shouldAllowMovement() {
        return allowMovement;
    }

    public static float getFadeAlpha() {
        if (!active || totalTicks <= 0) {
            return 0.0F;
        }

        float alpha = 0.0F;

        if (fadeInTicks > 0 && elapsedTicks < fadeInTicks) {
            float t = Mth.clamp(elapsedTicks / (float) fadeInTicks, 0.0F, 1.0F);
            alpha = Math.max(alpha, 1.0F - t);
        }

        if (fadeOutTicks > 0) {
            int remaining = totalTicks - elapsedTicks;

            if (remaining < fadeOutTicks) {
                float t = 1.0F - Mth.clamp(remaining / (float) fadeOutTicks, 0.0F, 1.0F);
                alpha = Math.max(alpha, t);
            }
        }

        return Mth.clamp(alpha, 0.0F, 1.0F);
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
        t = Mth.clamp(t, 0.0F, 1.0F);

        return switch (easing) {
            case 0 -> t;
            case 2 -> t * t;
            case 3 -> 1.0F - (1.0F - t) * (1.0F - t);
            case 4 -> {
                if (t < 0.5F) {
                    yield 2.0F * t * t;
                }

                yield 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 2.0F) / 2.0F;
            }
            case 1 -> t * t * (3.0F - 2.0F * t);
            default -> t * t * (3.0F - 2.0F * t);
        };
    }
}