package io.github.bakedlibs.dough.skins;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;

/**
 * Patched to wrap {@link GameProfile} rather than extend it.
 * In Paper 26+, {@link GameProfile} is {@code final} and cannot be subclassed,
 * causing {@link IncompatibleClassChangeError} at class-load time with the original.
 */
public final class CustomGameProfile {

    private static final String PLAYER_NAME = "CS-CoreLib";

    private final UUID id;
    private final GameProfile gameProfile;
    private final URL skinUrl;
    private final String texture;

    CustomGameProfile(@Nonnull UUID id, @Nullable String texture, @Nullable URL skinUrl) {
        this.id = id;
        this.gameProfile = new GameProfile(id, PLAYER_NAME);
        this.skinUrl = skinUrl;
        this.texture = texture;
        // authlib 7.0.63+ (Paper 26): GameProfile is a record with an immutable PropertyMap.
        // We cannot put() into it. Item skulls use apply() via Bukkit PlayerProfile API instead;
        // block skulls use applyToBlock() below — neither path needs properties on the GameProfile.
    }

    /**
     * Returns the underlying {@link GameProfile} for use where a {@link GameProfile} is required.
     */
    @Nonnull
    public GameProfile toGameProfile() {
        return gameProfile;
    }

    @Nullable
    public String getBase64Texture() {
        return texture;
    }

    void apply(@Nonnull SkullMeta meta) {
        if (skinUrl == null) {
            return;
        }

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(id, PLAYER_NAME);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(skinUrl);
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean applyToBlock(@Nonnull Block block, boolean update) {
        if (skinUrl == null) {
            return false;
        }

        try {
            if (!(block.getState() instanceof Skull skull)) {
                return false;
            }

            // Build base64 texture JSON directly, matching what the old NMS path stored.
            // Using Paper's ProfileProperty lets us skip Bukkit's profile-resolution layer,
            // which would fail for synthetic UUIDs that Mojang has never seen.
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + skinUrl + "\"}}}";
            String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            com.destroystokyo.paper.profile.PlayerProfile profile =
                Bukkit.createProfile(id, PLAYER_NAME);
            profile.setProperty(new ProfileProperty("textures", base64));
            skull.setOwnerProfile(profile);
            skull.update(update, false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
