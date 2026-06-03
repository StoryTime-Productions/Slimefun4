package io.github.thebusybiscuit.slimefun4.utils.compatibility;

import java.lang.reflect.Field;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.potion.PotionEffectType;

// https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/legacy/FieldRename.java?until=2a6207fe150b6165722fce94c83cc1f206620ab5&untilPath=src%2Fmain%2Fjava%2Forg%2Fbukkit%2Fcraftbukkit%2Flegacy%2FFieldRename.java#216-228
public class VersionedPotionEffectType {

    public static final PotionEffectType SLOWNESS;
    public static final PotionEffectType HASTE;
    public static final PotionEffectType MINING_FATIGUE;
    public static final PotionEffectType STRENGTH;
    public static final PotionEffectType INSTANT_HEALTH;
    public static final PotionEffectType INSTANT_DAMAGE;
    public static final PotionEffectType JUMP_BOOST;
    public static final PotionEffectType NAUSEA;
    public static final PotionEffectType RESISTANCE;

    static {
        // Try MC 1.20.5+ names first, then fall back to old names for pre-1.20.5 servers.
        // This avoids depending on Slimefun's MinecraftVersion detection, which returns UNKNOWN
        // for Paper 26+ (version string "26.1.2" not in Slimefun's version enum), causing all
        // isAtLeast(MINECRAFT_1_20_5) checks to return false and old names to be used — but
        // the old names (SLOW, HARM, JUMP, etc.) no longer exist in Paper 26's PotionEffectType.
        SLOWNESS      = getKeyOrFallback("SLOWNESS",      "SLOW");
        HASTE         = getKeyOrFallback("HASTE",         "FAST_DIGGING");
        MINING_FATIGUE = getKeyOrFallback("MINING_FATIGUE", "SLOW_DIGGING");
        STRENGTH      = getKeyOrFallback("STRENGTH",      "INCREASE_DAMAGE");
        INSTANT_HEALTH = getKeyOrFallback("INSTANT_HEALTH", "HEAL");
        INSTANT_DAMAGE = getKeyOrFallback("INSTANT_DAMAGE", "HARM");
        JUMP_BOOST    = getKeyOrFallback("JUMP_BOOST",    "JUMP");
        NAUSEA        = getKeyOrFallback("NAUSEA",        "CONFUSION");
        RESISTANCE    = getKeyOrFallback("RESISTANCE",    "DAMAGE_RESISTANCE");
    }

    @Nullable
    private static PotionEffectType getKeyOrFallback(@Nonnull String newName, @Nonnull String oldName) {
        PotionEffectType type = getKey(newName);
        return type != null ? type : getKey(oldName);
    }

    @Nullable
    private static PotionEffectType getKey(@Nonnull String key) {
        try {
            Field field = PotionEffectType.class.getDeclaredField(key);
            return (PotionEffectType) field.get(null);
        } catch(Exception e) {
            return null;
        }
    }
}
