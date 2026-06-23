package org.crafterscr.crafterscam.client;

public record CameraFrame(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        float roll,
        float fov
) {
}