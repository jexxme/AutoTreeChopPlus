package org.atcplus.autotreechopplus.utils;

import org.bukkit.entity.Player;
import org.atcplus.autotreechopplus.AutoTreeChopPlus;
import org.atcplus.autotreechopplus.Config;

import java.util.HashMap;
import java.util.UUID;

public class CooldownManager {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final AutoTreeChopPlus plugin;

    public CooldownManager(AutoTreeChopPlus plugin) {
        this.plugin = plugin;
    }


    public void setCooldown(Player player, UUID playerUUID, Config config) {
        if (player.hasPermission("atcplus.vip")) {
            cooldowns.put(playerUUID, System.currentTimeMillis() + (config.getVipCooldownTime() * 1000L));
        } else {
            cooldowns.put(playerUUID, System.currentTimeMillis() + (config.getCooldownTime() * 1000L));
        }
    }

    public boolean isInCooldown(UUID playerUUID) {
        Long cooldownEnd = cooldowns.get(playerUUID);
        if (cooldownEnd == null) {
            return false;
        }
        return System.currentTimeMillis() < cooldownEnd;
    }

    public long getRemainingCooldown(UUID playerUUID) {
        Long cooldownEnd = cooldowns.get(playerUUID);
        if (cooldownEnd == null) {
            return 0;
        }
        long remainingTime = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remainingTime / 1000);
    }
}




