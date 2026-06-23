package org.crafterscr.crafterscam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.crafterscr.crafterscam.CraftersCam;

import java.util.ArrayList;
import java.util.List;

public record StartCinematicPayload(List<NetCameraSegment> segments) implements CustomPacketPayload {
    public static final Type<StartCinematicPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CraftersCam.MOD_ID, "start_cinematic")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, StartCinematicPayload> STREAM_CODEC =
            StreamCodec.ofMember(StartCinematicPayload::write, StartCinematicPayload::read);

    public StartCinematicPayload {
        segments = List.copyOf(segments);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(segments.size());

        for (NetCameraSegment segment : segments) {
            segment.write(buffer);
        }
    }

    private static StartCinematicPayload read(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<NetCameraSegment> segments = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            segments.add(NetCameraSegment.read(buffer));
        }

        return new StartCinematicPayload(segments);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}