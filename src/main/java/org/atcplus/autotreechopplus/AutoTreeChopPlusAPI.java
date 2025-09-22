package org.atcplus.autotreechopplus;

import org.bukkit.entity.Player;

import java.util.UUID;

public class AutoTreeChopPlusAPI {

    private final AutoTreeChopPlus plugin;

    public AutoTreeChopPlusAPI(AutoTreeChopPlus plugin) {
        this.plugin = plugin;
    }

    /**
     * Get if AutoTreeChopPlus is enabled
     *
     * @return boolean
     */
    public boolean isAutoTreeChopPlusEnabled(Player player) {
        PlayerConfig playerConfig = plugin.getPlayerConfig(player.getUniqueId());
        return playerConfig.isAutoTreeChopPlusEnabled();
    }

    /**
     * Set specific player AutoTreeChopPlus as enabled
     */
    public void enableAutoTreeChopPlus(Player player) {
        PlayerConfig playerConfig = plugin.getPlayerConfig(player.getUniqueId());
        playerConfig.setAutoTreeChopPlusEnabled(true);
    }

    /**
     * Set specific player AutoTreeChopPlus as disable
     */
    public void disableAutoTreeChopPlus(Player player) {
        PlayerConfig playerConfig = plugin.getPlayerConfig(player.getUniqueId());
        playerConfig.setAutoTreeChopPlusEnabled(false);
    }

    /**
     * Get how many times player use AutoTreeChopPlus today
     *
     * @return int
     */
    public int getPlayerDailyUses(UUID playerUUID) {
        return plugin.getPlayerDailyUses(playerUUID);
    }

    /**
     * Get how many blocks player break via AutoTreeChopPlus today
     *
     * @return int
     */
    public int getPlayerDailyBlocksBroken(UUID playerUUID) {
        return plugin.getPlayerDailyBlocksBroken(playerUUID);
    }
}





