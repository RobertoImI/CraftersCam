package org.crafterscr.crafterscam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.crafterscr.crafterscam.CraftersCam;

import java.util.ArrayList;
import java.util.List;

public record CameraPathPayload(
        boolean visible,
        String sequenceId,
        String dimension,
        List<NetCameraSegment> segments
) implements CustomPacketPayload {
    public static final Type<CameraPathPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CraftersCam.MOD_ID, "camera_path")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CameraPathPayload> STREAM_CODEC =
            StreamCodec.ofMember(CameraPathPayload::write, CameraPathPayload::read);

    public CameraPathPayload {
        sequenceId = sequenceId == null ? "" : sequenceId;
        dimension = dimension == null ? "" : dimension;
        segments = segments == null ? List.of() : List.copyOf(segments);
    }

    public static CameraPathPayload show(String sequenceId, String dimension, List<NetCameraSegment> segments) {
        return new CameraPathPayload(true, sequenceId, dimension, segments);
    }

    public static CameraPathPayload hidden() {
        return new CameraPathPayload(false, "", "", List.of());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(visible);
        buffer.writeUtf(sequenceId);
        buffer.writeUtf(dimension);
        buffer.writeVarInt(segments.size());

        for (NetCameraSegment segment : segments) {
            segment.write(buffer);
        }
    }

    private static CameraPathPayload read(RegistryFriendlyByteBuf buffer) {
        boolean visible = buffer.readBoolean();
        String sequenceId = buffer.readUtf();
        String dimension = buffer.readUtf();

        int size = buffer.readVarInt();
        List<NetCameraSegment> segments = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            segments.add(NetCameraSegment.read(buffer));
        }

        return new CameraPathPayload(visible, sequenceId, dimension, segments);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
