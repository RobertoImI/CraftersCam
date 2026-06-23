package org.crafterscr.crafterscam.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playBidirectional(
                StartCinematicPayload.TYPE,
                StartCinematicPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler::handleStart,
                        (payload, context) -> {
                        }
                )
        );

        registrar.playBidirectional(
                StopCinematicPayload.TYPE,
                StopCinematicPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler::handleStop,
                        (payload, context) -> {
                        }
                )
        );
    }
}