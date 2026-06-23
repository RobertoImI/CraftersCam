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

    private static class ClientOnly {
        private static void start(StartCinematicPayload payload) {
            org.crafterscr.crafterscam.client.ClientCinematicController.start(payload.segments());
        }

        private static void stop() {
            org.crafterscr.crafterscam.client.ClientCinematicController.stop();
        }
    }
}