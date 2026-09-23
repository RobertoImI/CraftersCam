package org.crafterscr.crafterscam.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.crafterscr.crafterscam.CraftersCam;
import org.crafterscr.crafterscam.network.CameraPathPayload;
import org.crafterscr.crafterscam.network.NetCameraPoint;
import org.crafterscr.crafterscam.network.NetCameraSegment;

import java.util.List;
import java.util.Objects;

@EventBusSubscriber(modid = CraftersCam.MOD_ID, value = Dist.CLIENT)
public final class ClientCameraPathRenderer {
    private static final float MOVE_R = 0.20F;
    private static final float MOVE_G = 0.85F;
    private static final float MOVE_B = 1.00F;

    private static final float JUMP_R = 1.00F;
    private static final float JUMP_G = 0.25F;
    private static final float JUMP_B = 0.25F;

    private static final float POINT_R = 1.00F;
    private static final float POINT_G = 1.00F;
    private static final float POINT_B = 1.00F;

    private static final float LOOK_R = 1.00F;
    private static final float LOOK_G = 0.78F;
    private static final float LOOK_B = 0.12F;

    private static boolean visible = false;
    private static String sequenceId = "";
    private static String dimension = "";
    private static List<NetCameraSegment> segments = List.of();

    private ClientCameraPathRenderer() {
    }

    public static void apply(CameraPathPayload payload) {
        if (!payload.visible()) {
            hide();
            return;
        }

        visible = true;
        sequenceId = payload.sequenceId();
        dimension = payload.dimension();
        segments = List.copyOf(payload.segments());
    }

    public static void hide() {
        visible = false;
        sequenceId = "";
        dimension = "";
        segments = List.of();
    }

    public static boolean isVisible() {
        return visible;
    }

    public static String getSequenceId() {
        return sequenceId;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!visible || segments.isEmpty()) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        // Durante una reproducción/preview no mostramos las guías de edición.
        if (ClientCinematicController.isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        String currentDimension = minecraft.level.dimension().location().toString();

        if (!Objects.equals(currentDimension, dimension)) {
            return;
        }

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();

        NetCameraPoint previousEnd = null;

        for (NetCameraSegment segment : segments) {
            NetCameraPoint from = segment.from();
            NetCameraPoint to = segment.to();

            // Si un paso empieza lejos de donde terminó el anterior, la cámara
            // realmente hace un salto instantáneo. Lo marcamos discontinuo.
            if (previousEnd != null && !samePosition(previousEnd, from)) {
                drawDashedLine(
                        poseStack,
                        lines,
                        cameraPos,
                        toVec(previousEnd),
                        toVec(from),
                        JUMP_R,
                        JUMP_G,
                        JUMP_B,
                        0.95F
                );
            }

            if (segment.type() == 1) {
                // MOVE actual de CraftersCam: trayectoria espacial lineal.
                drawLine(
                        poseStack,
                        lines,
                        cameraPos,
                        toVec(from),
                        toVec(to),
                        MOVE_R,
                        MOVE_G,
                        MOVE_B,
                        0.95F
                );
            }

            // CUT explícito: punto rojo. Si venía desde otra posición, la línea
            // discontinua anterior deja claro que no existe movimiento entre ambos.
            if (segment.type() == 2) {
                drawCross(
                        poseStack,
                        lines,
                        cameraPos,
                        toVec(from),
                        0.22D,
                        JUMP_R,
                        JUMP_G,
                        JUMP_B,
                        1.0F
                );
            } else {
                drawCross(
                        poseStack,
                        lines,
                        cameraPos,
                        toVec(from),
                        0.16D,
                        POINT_R,
                        POINT_G,
                        POINT_B,
                        1.0F
                );
            }

            drawLookDirection(poseStack, lines, cameraPos, from);

            if (segment.type() == 1) {
                drawCross(
                        poseStack,
                        lines,
                        cameraPos,
                        toVec(to),
                        0.16D,
                        POINT_R,
                        POINT_G,
                        POINT_B,
                        1.0F
                );
                drawLookDirection(poseStack, lines, cameraPos, to);
                previousEnd = to;
            } else {
                previousEnd = from;
            }
        }

        buffers.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static void drawLookDirection(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 cameraPos,
            NetCameraPoint point
    ) {
        Vec3 start = toVec(point);
        Vec3 direction = Vec3.directionFromRotation(point.pitch(), point.yaw());

        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }

        Vec3 end = start.add(direction.normalize().scale(1.35D));

        drawLine(
                poseStack,
                lines,
                cameraPos,
                start,
                end,
                LOOK_R,
                LOOK_G,
                LOOK_B,
                0.90F
        );
    }

    private static void drawCross(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 cameraPos,
            Vec3 center,
            double radius,
            float r,
            float g,
            float b,
            float a
    ) {
        drawLine(poseStack, lines, cameraPos,
                center.add(-radius, 0.0D, 0.0D),
                center.add(radius, 0.0D, 0.0D),
                r, g, b, a);

        drawLine(poseStack, lines, cameraPos,
                center.add(0.0D, -radius, 0.0D),
                center.add(0.0D, radius, 0.0D),
                r, g, b, a);

        drawLine(poseStack, lines, cameraPos,
                center.add(0.0D, 0.0D, -radius),
                center.add(0.0D, 0.0D, radius),
                r, g, b, a);
    }

    private static void drawDashedLine(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 cameraPos,
            Vec3 start,
            Vec3 end,
            float r,
            float g,
            float b,
            float a
    ) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();

        if (length < 1.0E-6D) {
            return;
        }

        Vec3 direction = delta.scale(1.0D / length);
        double dashLength = 0.35D;
        double gapLength = 0.22D;

        // Evita generar miles de vértices si existe un CUT enorme entre dos puntos.
        double patternLength = dashLength + gapLength;
        int estimatedDashes = Math.max(1, (int) Math.ceil(length / patternLength));
        int maxDashes = 256;

        if (estimatedDashes > maxDashes) {
            patternLength = length / maxDashes;
            dashLength = patternLength * 0.62D;
            gapLength = patternLength - dashLength;
        }

        double cursor = 0.0D;

        while (cursor < length) {
            double dashEnd = Math.min(length, cursor + dashLength);

            Vec3 aPos = start.add(direction.scale(cursor));
            Vec3 bPos = start.add(direction.scale(dashEnd));

            drawLine(poseStack, lines, cameraPos, aPos, bPos, r, g, b, a);
            cursor += dashLength + gapLength;
        }
    }

    private static void drawLine(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 cameraPos,
            Vec3 worldStart,
            Vec3 worldEnd,
            float r,
            float g,
            float b,
            float a
    ) {
        Vec3 start = worldStart.subtract(cameraPos);
        Vec3 end = worldEnd.subtract(cameraPos);

        Vec3 normal = end.subtract(start);

        if (normal.lengthSqr() < 1.0E-8D) {
            normal = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            normal = normal.normalize();
        }

        lines.addVertex(
                        poseStack.last(),
                        (float) start.x,
                        (float) start.y,
                        (float) start.z
                )
                .setColor(r, g, b, a)
                .setNormal(
                        poseStack.last(),
                        (float) normal.x,
                        (float) normal.y,
                        (float) normal.z
                );

        lines.addVertex(
                        poseStack.last(),
                        (float) end.x,
                        (float) end.y,
                        (float) end.z
                )
                .setColor(r, g, b, a)
                .setNormal(
                        poseStack.last(),
                        (float) normal.x,
                        (float) normal.y,
                        (float) normal.z
                );
    }

    private static Vec3 toVec(NetCameraPoint point) {
        return new Vec3(point.x(), point.y(), point.z());
    }

    private static boolean samePosition(NetCameraPoint a, NetCameraPoint b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();

        return (dx * dx + dy * dy + dz * dz) < 1.0E-8D;
    }
}
