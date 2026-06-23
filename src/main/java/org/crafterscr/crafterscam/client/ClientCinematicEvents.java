package org.crafterscr.crafterscam.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.crafterscr.crafterscam.CraftersCam;

@EventBusSubscriber(modid = CraftersCam.MOD_ID, value = Dist.CLIENT)
public class ClientCinematicEvents {
    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        ClientCinematicController.beforeClientTick();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientCinematicController.tick();
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (ClientCinematicController.isActive()) {
            event.setFOV(ClientCinematicController.getCurrentFov());
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ClientCinematicController.isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!ClientCinematicController.isActive()) {
            return;
        }

        if (ClientCinematicController.shouldHideHudAll()) {
            event.setCanceled(true);
            return;
        }

        if (VanillaGuiLayers.CROSSHAIR.equals(event.getName())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!ClientCinematicController.isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        GuiGraphics graphics = event.getGuiGraphics();

        if (ClientCinematicController.shouldShowBars()) {
            int barHeight = Math.max(18, height / 9);

            graphics.fill(0, 0, width, barHeight, 0xFF000000);
            graphics.fill(0, height - barHeight, width, height, 0xFF000000);
        }

        float fadeAlpha = ClientCinematicController.getFadeAlpha();

        if (fadeAlpha > 0.01F) {
            int alpha = Math.max(0, Math.min(255, (int) (fadeAlpha * 255.0F)));
            int color = (alpha << 24);

            graphics.fill(0, 0, width, height, color);
        }
    }
}