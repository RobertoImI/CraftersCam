package org.crafterscr.crafterscam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.crafterscr.crafterscam.CraftersCam;

import java.util.ArrayList;
import java.util.List;

public record CameraPointGuidePayload(
        boolean visible,
        List<GuidePoint> points
) implements CustomPacketPayload {
    public static final Type<CameraPointGuidePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CraftersCam.MOD_ID, "camera_point_guide")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CameraPointGuidePayload> STREAM_CODEC =
            StreamCodec.ofMember(CameraPointGuidePayload::write, CameraPointGuidePayload::read);

    public CameraPointGuidePayload {
        points = points == null ? List.of() : List.copyOf(points);
    }

    public static CameraPointGuidePayload show(List<GuidePoint> points) {
        return new CameraPointGuidePayload(true, points);
    }

    public static CameraPointGuidePayload hidden() {
        return new CameraPointGuidePayload(false, List.of());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(visible);
        buffer.writeVarInt(points.size());

        for (GuidePoint point : points) {
            point.write(buffer);
        }
    }

    private static CameraPointGuidePayload read(RegistryFriendlyByteBuf buffer) {
        boolean visible = buffer.readBoolean();
        int size = buffer.readVarInt();
        List<GuidePoint> points = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            points.add(GuidePoint.read(buffer));
        }

        return new CameraPointGuidePayload(visible, points);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record GuidePoint(String dimension, NetCameraPoint point) {
        public GuidePoint {
            dimension = dimension == null ? "" : dimension;
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(dimension);
            point.write(buffer);
        }

        private static GuidePoint read(RegistryFriendlyByteBuf buffer) {
            return new GuidePoint(
                    buffer.readUtf(),
                    NetCameraPoint.read(buffer)
            );
        }
    }
}
