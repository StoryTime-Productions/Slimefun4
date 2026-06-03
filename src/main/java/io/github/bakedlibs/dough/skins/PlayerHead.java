package io.github.bakedlibs.dough.skins;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import io.github.bakedlibs.dough.skins.nms.PlayerHeadAdapter;

/**
 * Patched for Paper 26 compatibility:
 * - {@link CustomGameProfile} no longer extends {@link com.mojang.authlib.GameProfile} (now final).
 * - {@link #setSkin} tries the Bukkit {@code Skull.setOwnerProfile} API first before falling
 *   back to the NMS adapter (which is unavailable in Paper 26 due to class renames).
 */
public final class PlayerHead {

    private static final PlayerHeadAdapter adapter;

    static {
        adapter = PlayerHeadAdapter.get();
    }

    private PlayerHead() {}

    @Nonnull
    public static ItemStack getItemStack(@Nonnull OfflinePlayer player) {
        Validate.notNull(player, "The player can not be null!");
        return getItemStack(meta -> meta.setOwningPlayer(player));
    }

    @Nonnull
    public static ItemStack getItemStack(@Nonnull PlayerSkin skin) {
        Validate.notNull(skin, "The skin can not be null!");
        return getItemStack(meta -> {
            try {
                skin.getProfile().apply(meta);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Nonnull
    private static ItemStack getItemStack(@Nonnull Consumer<SkullMeta> consumer) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        consumer.accept(meta);
        skull.setItemMeta(meta);
        return skull;
    }

    public static void setSkin(@Nonnull Block block, @Nonnull PlayerSkin skin, boolean update) {
        Material type = block.getType();
        if (type != Material.PLAYER_HEAD && type != Material.PLAYER_WALL_HEAD) {
            throw new IllegalArgumentException("Cannot update a head texture. Expected a Player Head, received: " + type);
        }

        CustomGameProfile profile = skin.getProfile();

        // Try Bukkit API first (Paper 26+: NMS TileEntitySkull is no longer accessible)
        if (profile.applyToBlock(block, update)) {
            return;
        }

        if (adapter == null) {
            throw new UnsupportedOperationException("Cannot update skin texture, no adapter found");
        }

        try {
            adapter.setGameProfile(block, profile.toGameProfile(), update);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }
}
