package org.crafterscr.crafterscam.server;

import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class CamServerEvents {
    public static void onServerTick(ServerTickEvent.Post event) {
        CamServerManager.INSTANCE.tickShowPoints(event.getServer());
    }
}