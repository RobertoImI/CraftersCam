package org.crafterscr.crafterscam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

public record NetCameraSegment(
        int type,
        NetCameraPoint from,
        NetCameraPoint to,
        int durationTicks,
        int easing
) {
    public static NetCameraSegment hold(NetCameraPoint point, int durationTicks) {
        return new NetCameraSegment(0, point, point, Math.max(1, durationTicks), 1);
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(type);
        from.write(buffer);
        to.write(buffer);
        buffer.writeVarInt(Math.max(1, durationTicks));
        buffer.writeVarInt(easing);
    }

    public static NetCameraSegment read(RegistryFriendlyByteBuf buffer) {
        return new NetCameraSegment(
                buffer.readVarInt(),
                NetCameraPoint.read(buffer),
                NetCameraPoint.read(buffer),
                Math.max(1, buffer.readVarInt()),
                buffer.readVarInt()
        );
    }
}