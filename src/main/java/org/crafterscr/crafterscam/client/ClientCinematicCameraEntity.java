package org.crafterscr.crafterscam.client;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.Level;

public class ClientCinematicCameraEntity extends Marker {
    public ClientCinematicCameraEntity(Level level) {
        super(EntityType.MARKER, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public void applyFrame(CameraFrame frame) {
        this.setPos(frame.x(), frame.y(), frame.z());

        this.xo = frame.x();
        this.yo = frame.y();
        this.zo = frame.z();

        this.xOld = frame.x();
        this.yOld = frame.y();
        this.zOld = frame.z();

        this.setYRot(frame.yaw());
        this.setXRot(frame.pitch());

        this.yRotO = frame.yaw();
        this.xRotO = frame.pitch();
    }

    @Override
    public boolean isInvisible() {
        return true;
    }
}