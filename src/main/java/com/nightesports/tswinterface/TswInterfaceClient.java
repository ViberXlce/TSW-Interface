package com.nightesports.tswinterface;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;

public class TswInterfaceClient implements ClientModInitializer {
    public static final String MOD_ID = "tsw_interface";

    /** Opens the Quest Log (default: L). */
    public static KeyMapping QUEST_KEY;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        QUEST_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.tsw_interface.quests", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, category));

        ClientTickEvents.END_CLIENT_TICK.register(TswInterfaceClient::onTick);
        TswHud.register();
    }

    private static void onTick(Minecraft mc) {
        if (mc.player == null) {
            return;
        }

        // swap the vanilla survival inventory for the TSW screen within the same tick
        if (mc.screen instanceof InventoryScreen && !mc.player.isCreative()) {
            mc.setScreen(new TswInventoryScreen(mc.player));
        }

        // L is also the vanilla advancements key: swap that screen for the Quest Log
        if (mc.screen instanceof AdvancementsScreen) {
            mc.setScreen(new QuestLogScreen());
        }

        // L = quest log
        while (QUEST_KEY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new QuestLogScreen());
            }
        }
    }
}
