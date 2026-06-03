package io.github.bakedlibs.dough.skins.nms;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.block.Block;

import com.mojang.authlib.GameProfile;

import io.github.bakedlibs.dough.versions.MinecraftVersion;

/**
 * Patched to suppress the SEVERE log when NMS TileEntitySkull is unavailable on Paper 26+.
 * The NMS-based adapters are not needed on Paper 26 because PlayerHead.setSkin() uses the
 * Bukkit Skull.setOwnerProfile() API first (via CustomGameProfile.applyToBlock()).
 */
public interface PlayerHeadAdapter {

    void setGameProfile(Block block, GameProfile profile, boolean update)
        throws IllegalAccessException, InvocationTargetException, InstantiationException;

    static PlayerHeadAdapter get() {
        try {
            MinecraftVersion version = MinecraftVersion.get();
            if (version.isAtLeast(1, 20, 5)) {
                return new PlayerHeadAdapter20v5();
            } else if (version.isAtLeast(1, 18)) {
                return new PlayerHeadAdapter18();
            } else if (version.isAtLeast(1, 17)) {
                return new PlayerHeadAdapter17();
            } else {
                return new PlayerHeadAdapterBefore17();
            }
        } catch (Exception e) {
            // NMS skull class inaccessible (Paper 26+ restricts TileEntitySkull).
            // Block skulls are handled via Bukkit API; return null silently.
            return null;
        }
    }
}
