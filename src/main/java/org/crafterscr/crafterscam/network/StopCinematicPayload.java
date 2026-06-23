package org.crafterscr.crafterscam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.crafterscr.crafterscam.CraftersCam;

public record StopCinematicPayload() implements CustomPacketPayload {
    public static final StopCinematicPayload INSTANCE = new StopCinematicPayload();

    public static final Type<StopCinematicPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CraftersCam.MOD_ID, "stop_cinematic")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, StopCinematicPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}