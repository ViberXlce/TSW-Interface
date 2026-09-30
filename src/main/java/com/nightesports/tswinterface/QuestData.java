package com.nightesports.tswinterface;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Sample quest data for the Quest Log screen. Later this list will be filled from the server.
 */
final class QuestData {

    enum Status {
        NEW, ACTIVE, TRACKED, COMPLETED
    }

    static final class Quest {
        final String title;
        final Item icon;
        final String objective;
        final int goal;
        final int progress;
        final String rewards;
        Status status;

        Quest(String title, Item icon, String objective, int progress, int goal, String rewards, Status status) {
            this.title = title;
            this.icon = icon;
            this.objective = objective;
            this.progress = progress;
            this.goal = goal;
            this.rewards = rewards;
            this.status = status;
        }
    }

    static final List<Quest> QUESTS = new ArrayList<>(List.of(
            new Quest("EMERALD HEIST", Items.EMERALD, "Gather 50 Emeralds", 24, 50, "XP, Gold", Status.ACTIVE),
            new Quest("DEFEND THE VILLAGE", Items.IRON_SWORD, "Repel Raider Raid", 0, 0, "Reputation, XP", Status.ACTIVE),
            new Quest("SLAY THE WYVERN", Items.DRAGON_HEAD, "Defeat Wyvern in Caves", 0, 0, "Armor, XP", Status.ACTIVE),
            new Quest("THE FORBIDDEN CAVE", Items.SPIDER_EYE, "Slay Cave Spider Queen", 0, 0, "Silk, Sword", Status.ACTIVE),
            new Quest("ANCIENT RUINS", Items.CHISELED_STONE_BRICKS, "Discover Forgotten Chamber", 0, 0, "Artifact, Lvl Up", Status.ACTIVE),
            new Quest("CRAFTING CHALLENGE", Items.ENCHANTING_TABLE, "Build 1 Enchanting Table", 0, 0, "Enchantment Book", Status.NEW),
            new Quest("HARVEST HARMONY", Items.WHEAT, "Deliver 64 Wheat", 64, 64, "Bread", Status.COMPLETED),
            new Quest("RARE MINERALS", Items.NETHERITE_PICKAXE, "Mine 20 Netherite Scrap", 0, 20, "Pickaxe, XP", Status.TRACKED),
            new Quest("ZOMBIE HUNT", Items.ROTTEN_FLESH, "Kill 50 Zombies", 12, 50, "Mining XP", Status.NEW),
            new Quest("REDSTONE RUSH", Items.REDSTONE, "Collect 128 Redstone", 40, 128, "Gold, XP", Status.NEW),
            new Quest("DEEP MINER", Items.DIAMOND_PICKAXE, "Reach Mining Level 10", 4, 10, "Double Drop", Status.NEW),
            new Quest("MASTER ANGLER", Items.FISHING_ROD, "Catch 30 Fish", 9, 30, "Luck Point", Status.NEW)
    ));

    private QuestData() {
    }
}
