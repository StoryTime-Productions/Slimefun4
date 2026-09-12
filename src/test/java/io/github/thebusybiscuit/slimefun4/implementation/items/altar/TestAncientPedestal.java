package io.github.thebusybiscuit.slimefun4.implementation.items.altar;

import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.setup.SlimefunItemSetup;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.ItemEntityMock;

/**
 * Regression test for the {@link AncientPedestal} lore-wipe bug (see specs/ancient-altar-nbt-preservation.md).
 * Uses a manually-built entity instead of a full placeItem() integration test because MockBukkit
 * doesn't yet implement LivingEntity#setRemoveWhenFarAway, which ArmorStandUtils.spawnArmorStand needs.
 */
class TestAncientPedestal {

    private static Slimefun plugin;
    private static ServerMock server;
    private static AncientPedestal pedestalItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        SlimefunItemSetup.setup(plugin);
        pedestalItem = (AncientPedestal) SlimefunItems.ANCIENT_PEDESTAL.getItem();
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Test that picking up an item placed on an Ancient Pedestal preserves its lore")
    void testGetOriginalItemStackPreservesLore() {
        String originalDisplayName = "§6My Sword";
        List<String> originalLore = List.of("§7A fine blade", "§7+5 Damage");

        ItemStack originalItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta originalMeta = originalItem.getItemMeta();
        originalMeta.setDisplayName(originalDisplayName);
        originalMeta.setLore(originalLore);
        originalItem.setItemMeta(originalMeta);

        String nametag = ItemUtils.getItemName(originalItem);

        ItemStack displayItem = originalItem.clone();
        ItemMeta displayMeta = displayItem.getItemMeta();
        displayMeta.setDisplayName(AncientPedestal.ITEM_PREFIX + System.nanoTime());
        displayItem.setItemMeta(displayMeta);

        Item entity = new ItemEntityMock(server, UUID.randomUUID(), displayItem);
        entity.setCustomName(nametag);

        ItemStack pickedUp = pedestalItem.getOriginalItemStack(entity);

        ItemMeta pickedUpMeta = pickedUp.getItemMeta();
        Assertions.assertTrue(pickedUpMeta.hasLore(), "Lore should not have been wiped when the item was placed on the pedestal");
        Assertions.assertEquals(originalLore, pickedUpMeta.getLore(), "Lore should be identical after a place -> remove round trip");
        Assertions.assertEquals(originalDisplayName, pickedUpMeta.getDisplayName(), "Display name should be restored after a place -> remove round trip");
    }

}
