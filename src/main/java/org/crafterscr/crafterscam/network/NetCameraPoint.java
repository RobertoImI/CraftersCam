package org.crafterscr.crafterscam.network;

import net.minecraft.network.RegistryFriendlyByteBuf;

public record NetCameraPoint(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        float roll,
        float fov
) {
    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        buffer.writeFloat(roll);
        buffer.writeFloat(fov);
    }

    public static NetCameraPoint read(RegistryFriendlyByteBuf buffer) {
        return new NetCameraPoint(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }
}