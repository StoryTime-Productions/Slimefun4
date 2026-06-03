package io.github.bakedlibs.dough.versions;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Server;

/**
 * Patched to handle Paper 26+ version strings like "26.1.2.build.63-stable".
 * The original parser splits by "-" to get "26.1.2.build.63" and then fails
 * because SemanticVersion.parse() expects exactly "major.minor.patch".
 * This patch strips extra dot-segments beyond the first three components.
 */
public class MinecraftVersion extends SemanticVersion {

    public MinecraftVersion(int major, int minor, int patch) {
        super(major, minor, patch);
    }

    private MinecraftVersion(@Nonnull SemanticVersion version) {
        super(version.getMajorVersion(), version.getMinorVersion(), version.getPatchVersion());
    }

    @Nonnull
    public static MinecraftVersion of(@Nonnull Server server) throws UnknownServerVersionException {
        Validate.notNull(server, "Server should not be null!");
        String bukkit = server.getBukkitVersion();
        try {
            String versionPart = bukkit.split("-")[0];
            // Paper 26 appends ".build.N" (e.g. "26.1.2.build.63") — keep only major.minor.patch
            String[] dotParts = versionPart.split("\\.");
            if (dotParts.length > 3) {
                versionPart = dotParts[0] + "." + dotParts[1] + "." + dotParts[2];
            }
            return new MinecraftVersion(SemanticVersion.parse(versionPart));
        } catch (Exception e) {
            throw new UnknownServerVersionException(bukkit, e);
        }
    }

    @Nonnull
    public static MinecraftVersion get() throws UnknownServerVersionException {
        return of(Bukkit.getServer());
    }

    public static boolean isMocked(@Nonnull Server server) {
        Class<?> clazz = server.getClass();
        while (clazz != null) {
            if (clazz.getName().endsWith("mockbukkit.ServerMock")) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    public static boolean isMocked() {
        return isMocked(Bukkit.getServer());
    }

    @Override
    public String getAsString() {
        return "MC: " + super.getAsString();
    }
}
