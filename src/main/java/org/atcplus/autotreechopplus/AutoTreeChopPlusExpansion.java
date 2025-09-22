package org.atcplus.autotreechopplus;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class AutoTreeChopPlusExpansion extends PlaceholderExpansion {

    private final AutoTreeChopPlus plugin;

    public AutoTreeChopPlusExpansion(AutoTreeChopPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "autotreechopplus";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        UUID playerUUID = player.getUniqueId();

        if (params.equalsIgnoreCase("daily_uses")) {
            return String.valueOf(plugin.getPlayerDailyUses(playerUUID));
        } else if (params.equalsIgnoreCase("daily_blocks_broken")) {
            return String.valueOf(plugin.getPlayerDailyBlocksBroken(playerUUID));
        } else if (params.equalsIgnoreCase("status")) {
            return String.valueOf(plugin.getPlayerConfig(playerUUID).isAutoTreeChopPlusEnabled());
        }

        return null;
    }
}






