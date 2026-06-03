package io.github.thebusybiscuit.slimefun4.utils.compatibility;

import java.lang.reflect.Field;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.inventory.ItemFlag;

import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

public class VersionedItemFlag {
    
    public static final ItemFlag HIDE_ADDITIONAL_TOOLTIP;

    static {
        MinecraftVersion version = Slimefun.getMinecraftVersion();

        if (version.isAtLeast(MinecraftVersion.MINECRAFT_1_20_5)) {
            HIDE_ADDITIONAL_TOOLTIP = ItemFlag.HIDE_ADDITIONAL_TOOLTIP;
        } else {
            // For unknown/future versions (e.g. Paper 26 reports MC 26.1.2 which Slimefun
            // doesn't recognize), HIDE_POTION_EFFECTS may no longer exist. Try the new name
            // first, then fall back to the old name for genuinely older servers.
            ItemFlag flag = getKey("HIDE_ADDITIONAL_TOOLTIP");
            if (flag == null) {
                flag = getKey("HIDE_POTION_EFFECTS");
            }
            HIDE_ADDITIONAL_TOOLTIP = flag;
        }
    }

    @Nullable
    private static ItemFlag getKey(@Nonnull String key) {
        try {
            Field field = ItemFlag.class.getDeclaredField(key);
            return (ItemFlag) field.get(null);
        } catch(Exception e) {
            return null;
        }
    }
}
