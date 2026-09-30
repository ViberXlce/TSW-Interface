package com.nightesports.tswinterface;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public class TswInterfaceClient implements ClientModInitializer {
    public static final String MOD_ID = "tsw_interface";

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(TswInterfaceClient::onTick);
    }

    /**
     * Whenever the vanilla survival inventory is opened (E key), swap it for the TSW screen
     * within the same tick, so the vanilla UI is never rendered.
     */
    private static void onTick(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        if (mc.screen instanceof InventoryScreen && !mc.player.isCreative()) {
            mc.setScreen(new TswInventoryScreen(mc.player));
        }
    }
}
