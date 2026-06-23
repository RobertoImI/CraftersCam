package org.crafterscr.crafterscam;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.crafterscr.crafterscam.command.CamCommands;
import org.crafterscr.crafterscam.network.ModNetworking;
import org.crafterscr.crafterscam.server.CamServerEvents;
import org.slf4j.Logger;

@Mod(CraftersCam.MOD_ID)
public class CraftersCam {
    public static final String MOD_ID = "crafterscam";

    // Compatibilidad con Config.java del template.
    public static final String MODID = MOD_ID;

    public static final Logger LOGGER = LogUtils.getLogger();

    public CraftersCam(IEventBus modEventBus) {
        modEventBus.addListener(ModNetworking::register);

        NeoForge.EVENT_BUS.addListener(CamCommands::register);
        NeoForge.EVENT_BUS.addListener(CamServerEvents::onServerTick);
    }
}