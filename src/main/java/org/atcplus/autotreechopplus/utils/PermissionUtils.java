package org.atcplus.autotreechopplus.utils;

import org.bukkit.entity.Player;
import org.atcplus.autotreechopplus.Config;
import org.atcplus.autotreechopplus.PlayerConfig;

public class PermissionUtils {

    // VIP limit checker
    public static boolean hasVipUses(Player player, PlayerConfig playerConfig, Config config) {
        if (!config.getLimitVipUsage()) return player.hasPermission("atcplus.vip");
        if (player.hasPermission("atcplus.vip")) return playerConfig.getDailyUses() <= config.getVipUsesPerDay();
        return false;
    }

    public static boolean hasVipBlock(Player player, PlayerConfig playerConfig, Config config) {
        if (!config.getLimitVipUsage()) return player.hasPermission("atcplus.vip");
        if (player.hasPermission("atcplus.vip"))
            return playerConfig.getDailyBlocksBroken() <= config.getVipBlocksPerDay();
        return false;
    }
}




