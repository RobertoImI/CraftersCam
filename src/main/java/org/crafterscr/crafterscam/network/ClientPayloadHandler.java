package org.crafterscr.crafterscam.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleStart(StartCinematicPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                ClientOnly.start(payload);
            }
        });
    }

    public static void handleStop(StopCinematicPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                ClientOnly.stop();
            }
        });
    }

    public static void handlePath(CameraPathPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                ClientOnly.path(payload);
            }
        });
    }

    public static void handlePointGuide(CameraPointGuidePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist() == Dist.CLIENT) {
                ClientOnly.pointGuide(payload);
            }
        });
    }

    private static class ClientOnly {
        private static void start(StartCinematicPayload payload) {
            org.crafterscr.crafterscam.client.ClientCinematicController.start(
                    payload.segments(),
                    payload.fadeInTicks(),
                    payload.fadeOutTicks(),
                    payload.showBars(),
                    payload.hideHudAll(),
                    payload.allowMovement()
            );
        }

        private static void stop() {
            org.crafterscr.crafterscam.client.ClientCinematicController.stop();
        }

        private static void path(CameraPathPayload payload) {
            org.crafterscr.crafterscam.client.ClientCameraPathRenderer.apply(payload);
        }

        private static void pointGuide(CameraPointGuidePayload payload) {
            org.crafterscr.crafterscam.client.ClientCameraPathRenderer.applyPoints(payload);
        }
    }
}