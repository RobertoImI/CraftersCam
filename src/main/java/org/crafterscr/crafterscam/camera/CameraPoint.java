package org.crafterscr.crafterscam.camera;

import net.minecraft.server.level.ServerPlayer;
import org.crafterscr.crafterscam.network.NetCameraPoint;

public class CameraPoint {
    public String id = "";
    public String dimension = "minecraft:overworld";

    public double x;
    public double y;
    public double z;

    public float yaw;
    public float pitch;
    public float roll;
    public float fov = 70.0F;

    public CameraPoint() {
    }

    public CameraPoint(String id, String dimension, double x, double y, double z, float yaw, float pitch, float roll, float fov) {
        this.id = id;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.fov = fov;
    }

    public static CameraPoint fromPlayer(String id, ServerPlayer player, float fov) {
        return new CameraPoint(
                id,
                player.level().dimension().location().toString(),
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                0.0F,
                fov
        );
    }

    public NetCameraPoint toNetwork() {
        return new NetCameraPoint(x, y, z, yaw, pitch, roll, fov);
    }
}