package org.atcplus.autotreechopplus.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.atcplus.autotreechopplus.AutoTreeChopPlus;
import org.atcplus.autotreechopplus.Config;
import org.atcplus.autotreechopplus.PlayerConfig;
import org.atcplus.autotreechopplus.hooks.GriefPreventionHook;
import org.atcplus.autotreechopplus.hooks.LandsHook;
import org.atcplus.autotreechopplus.hooks.ResidenceHook;
import org.atcplus.autotreechopplus.hooks.WorldGuardHook;

import java.util.*;

public class LeafRemovalUtils {

    // Track removed logs during this tree chopping session
    private static final Map<String, Set<Location>> removedLogsPerSession = new HashMap<>();
    private static final Set<String> activeLeafRemovalSessions = new HashSet<>();

    /**
     * Initiates leaf removal process after tree chopping
     */
    public static void processLeafRemoval(Block originalLogBlock, Material originalLogType, Player player, AutoTreeChopPlus plugin,
                                          Config config, PlayerConfig playerConfig,
                                          boolean worldGuardEnabled, boolean residenceEnabled,
                                          boolean griefPreventionEnabled, boolean landsEnabled,
                                          LandsHook landsHook, ResidenceHook residenceHook,
                                          GriefPreventionHook griefPreventionHook, WorldGuardHook worldGuardHook) {

        // Check if leaf removal is enabled
        if (!config.isLeafRemovalEnabled()) {
            return;
        }

        // Check if player has permission
        if (!player.hasPermission("atcplus.leaves")) {
            return;
        }

        // Prevent multiple leaf removal sessions for the same player
        String playerKey = player.getUniqueId().toString();
        if (activeLeafRemovalSessions.contains(playerKey)) {
            return; // Already processing leaf removal for this player
        }

        // Create session ID for tracking removed logs
        String sessionId = playerKey + "_" + System.currentTimeMillis();
        removedLogsPerSession.put(sessionId, new HashSet<>());
        activeLeafRemovalSessions.add(playerKey);

        final boolean netherFungusContext = config.isNetherFungiEnabled() && (
                isNetherFungusLog(originalLogType)
                        || originalLogBlock.getWorld().getEnvironment() == World.Environment.NETHER);

        Runnable leafRemovalTask = () -> {
            Set<Location> checkedLeafLocations = new HashSet<>();
            Set<Location> processingLeafLocations = new HashSet<>();

            // Add a delay to ensure all tree logs are broken first
            long delayTicks = Math.max(config.getLeafRemovalDelayTicks(), 60L); // Minimum 3 seconds

            if (delayTicks > 0) {
                // Always use delayed removal to avoid conflicts with tree chopping
                scheduleDelayedLeafRemoval(originalLogBlock, netherFungusContext, player, plugin, config, playerConfig,
                        worldGuardEnabled, residenceEnabled, griefPreventionEnabled, landsEnabled,
                        landsHook, residenceHook, griefPreventionHook, worldGuardHook,
                        checkedLeafLocations, processingLeafLocations, sessionId, playerKey);
            } else {
                // Immediate removal (not recommended but kept for compatibility)
                startLeafRemoval(originalLogBlock, netherFungusContext, player, plugin, config, playerConfig,
                        worldGuardEnabled, residenceEnabled, griefPreventionEnabled, landsEnabled,
                        landsHook, residenceHook, griefPreventionHook, worldGuardHook,
                        checkedLeafLocations, processingLeafLocations, sessionId, playerKey);
            }
        };

        // Execute based on async configuration
        if (config.isLeafRemovalAsync() && !AutoTreeChopPlus.isFolia()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    Bukkit.getScheduler().runTask(plugin, leafRemovalTask));
        } else {
            leafRemovalTask.run();
        }
    }

    /**
     * Call this method from TreeChopUtils when a log is broken
     */
    public static void trackRemovedLog(Location logLocation, String playerUUID) {
        // Add to all active sessions for this player
        String playerKey = playerUUID;
        for (Map.Entry<String, Set<Location>> entry : removedLogsPerSession.entrySet()) {
            if (entry.getKey().startsWith(playerKey + "_")) {
                entry.getValue().add(logLocation.clone());
            }
        }
    }

    private static void scheduleDelayedLeafRemoval(Block originalLogBlock, boolean netherFungus, Player player, AutoTreeChopPlus plugin,
                                                   Config config, PlayerConfig playerConfig,
                                                   boolean worldGuardEnabled, boolean residenceEnabled,
                                                   boolean griefPreventionEnabled, boolean landsEnabled,
                                                   LandsHook landsHook, ResidenceHook residenceHook,
                                                   GriefPreventionHook griefPreventionHook, WorldGuardHook worldGuardHook,
                                                   Set<Location> checkedLeafLocations, Set<Location> processingLeafLocations,
                                                   String sessionId, String playerKey) {

        Runnable delayedTask = () -> startLeafRemoval(originalLogBlock, netherFungus, player, plugin, config, playerConfig,
                worldGuardEnabled, residenceEnabled, griefPreventionEnabled, landsEnabled,
                landsHook, residenceHook, griefPreventionHook, worldGuardHook,
                checkedLeafLocations, processingLeafLocations, sessionId, playerKey);

        if (AutoTreeChopPlus.isFolia()) {
            plugin.getServer().getRegionScheduler().runDelayed(plugin, originalLogBlock.getLocation(),
                    (task) -> delayedTask.run(), Math.max(config.getLeafRemovalDelayTicks(), 60L));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, delayedTask, Math.max(config.getLeafRemovalDelayTicks(), 60L));
        }
    }

    private static void startLeafRemoval(Block originalLogBlock, boolean netherFungus, Player player, AutoTreeChopPlus plugin,
                                         Config config, PlayerConfig playerConfig,
                                         boolean worldGuardEnabled, boolean residenceEnabled,
                                         boolean griefPreventionEnabled, boolean landsEnabled,
                                         LandsHook landsHook, ResidenceHook residenceHook,
                                         GriefPreventionHook griefPreventionHook, WorldGuardHook worldGuardHook,
                                         Set<Location> checkedLeafLocations, Set<Location> processingLeafLocations,
                                         String sessionId, String playerKey) {

        // Find all leaves within radius
        Collection<Block> leavesToRemove = findLeavesToRemove(originalLogBlock, config.getLeafRemovalRadius(),
                checkedLeafLocations, config, sessionId, netherFungus);

        if (leavesToRemove.isEmpty()) {
            // Clean up session
            cleanupSession(sessionId, playerKey);
            return;
        }

        // Process leaves in batches for performance
        List<Block> leavesList = new ArrayList<>(leavesToRemove);
        int batchSize = config.getLeafRemovalBatchSize();

        processLeavesBatch(leavesList, 0, batchSize, player, plugin, config, playerConfig,
                worldGuardEnabled, residenceEnabled, griefPreventionEnabled, landsEnabled,
                landsHook, residenceHook, griefPreventionHook, worldGuardHook, processingLeafLocations, sessionId, playerKey);
    }

    private static void processLeavesBatch(List<Block> leavesList, int startIndex, int batchSize,
                                           Player player, AutoTreeChopPlus plugin, Config config, PlayerConfig playerConfig,
                                           boolean worldGuardEnabled, boolean residenceEnabled,
                                           boolean griefPreventionEnabled, boolean landsEnabled,
                                           LandsHook landsHook, ResidenceHook residenceHook,
                                           GriefPreventionHook griefPreventionHook, WorldGuardHook worldGuardHook,
                                           Set<Location> processingLeafLocations, String sessionId, String playerKey) {

        int endIndex = Math.min(startIndex + batchSize, leavesList.size());
        int leavesRemovedThisBatch = 0;

        for (int i = startIndex; i < endIndex; i++) {
            Block leafBlock = leavesList.get(i);

            // Check block limit if leaf removal counts towards limit
            if (config.getLeafRemovalCountsTowardsLimit()) {
                if (!PermissionUtils.hasVipBlock(player, playerConfig, config) &&
                        playerConfig.getDailyBlocksBroken() >= config.getMaxBlocksPerDay()) {
                    break; // Hit the daily block limit
                }
            }

            if (removeLeafBlock(leafBlock, player, config, playerConfig, worldGuardEnabled, residenceEnabled,
                    griefPreventionEnabled, landsEnabled, landsHook, residenceHook,
                    griefPreventionHook, worldGuardHook, processingLeafLocations)) {
                leavesRemovedThisBatch++;
            }
        }

        // Schedule next batch if there are more leaves to process
        if (endIndex < leavesList.size() && leavesRemovedThisBatch > 0) {
            Runnable nextBatchTask = () -> processLeavesBatch(leavesList, endIndex, batchSize, player, plugin,
                    config, playerConfig, worldGuardEnabled, residenceEnabled, griefPreventionEnabled,
                    landsEnabled, landsHook, residenceHook, griefPreventionHook, worldGuardHook,
                    processingLeafLocations, sessionId, playerKey);

            if (AutoTreeChopPlus.isFolia()) {
                plugin.getServer().getRegionScheduler().runDelayed(plugin, leavesList.get(endIndex).getLocation(),
                        (task) -> nextBatchTask.run(), 1L);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, nextBatchTask, 1L);
            }
        } else if (endIndex >= leavesList.size()) {
            // Clean up session when done
            cleanupSession(sessionId, playerKey);
        }
    }

    private static void cleanupSession(String sessionId, String playerKey) {
        removedLogsPerSession.remove(sessionId);
        activeLeafRemovalSessions.remove(playerKey);
    }

    private static boolean removeLeafBlock(Block leafBlock, Player player, Config config, PlayerConfig playerConfig,
                                           boolean worldGuardEnabled, boolean residenceEnabled,
                                           boolean griefPreventionEnabled, boolean landsEnabled,
                                           LandsHook landsHook, ResidenceHook residenceHook,
                                           GriefPreventionHook griefPreventionHook, WorldGuardHook worldGuardHook,
                                           Set<Location> processingLeafLocations) {

        Location leafLocation = leafBlock.getLocation();

        // Skip if already processed
        if (processingLeafLocations.contains(leafLocation)) {
            return false;
        }

        // Permission checks using existing methods
        if (!TreeChopUtils.resCheck(player, leafLocation, residenceEnabled, residenceHook) ||
                !TreeChopUtils.landsCheck(player, leafLocation, landsEnabled, landsHook) ||
                !TreeChopUtils.gfCheck(player, leafLocation, griefPreventionEnabled, griefPreventionHook) ||
                !TreeChopUtils.wgCheck(player, leafLocation, worldGuardEnabled, worldGuardHook)) {
            return false;
        }

        processingLeafLocations.add(leafLocation);

        // Call BlockBreakEvent for the leaf block
        BlockBreakEvent breakEvent = new BlockBreakEvent(leafBlock, player);
        Bukkit.getPluginManager().callEvent(breakEvent);

        if (!breakEvent.isCancelled()) {
            // Show visual effect if enabled
            if (config.getLeafRemovalVisualEffects()) {
                EffectUtils.showLeafRemovalEffect(player, leafBlock);
            }

            // Break the leaf block
            if (config.getLeafRemovalDropItems()) {
                leafBlock.breakNaturally();
            } else {
                leafBlock.setType(Material.AIR);
            }

            // Update player stats if leaf removal counts towards block limit
            if (config.getLeafRemovalCountsTowardsLimit()) {
                playerConfig.incrementDailyBlocksBroken();
            }

            processingLeafLocations.remove(leafLocation);
            return true;
        }

        processingLeafLocations.remove(leafLocation);
        return false;
    }

    private static Collection<Block> findLeavesToRemove(Block centerBlock, int radius,
                                                        Set<Location> checkedLocations, Config config, String sessionId, boolean netherFungus) {
        Set<Block> leavesToRemove = new HashSet<>();
        Set<Location> visitedInThisSearch = new HashSet<>();
        Location center = centerBlock.getLocation();

        int scanRadius = radius;

        // Get removed logs for this session
        Set<Location> removedLogs = removedLogsPerSession.get(sessionId);
        if (removedLogs == null) {
            removedLogs = new HashSet<>();
        }

        // Find all leaf blocks within radius
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    Location checkLoc = center.clone().add(x, y, z);
                    Block checkBlock = checkLoc.getBlock();

                    if (isLeafBlock(checkBlock.getType(), config) &&
                            !checkedLocations.contains(checkLoc) &&
                            !visitedInThisSearch.contains(checkLoc)) {

                        // Use improved orphaned detection
                        if (isOrphanedLeaf(checkBlock, config, removedLogs, netherFungus)) {
                            leavesToRemove.add(checkBlock);
                        }
                        visitedInThisSearch.add(checkLoc);
                    }
                }
            }
        }

        checkedLocations.addAll(visitedInThisSearch);
        return leavesToRemove;
    }

    private static boolean isOrphanedLeaf(Block leafBlock, Config config, Set<Location> removedLogs, boolean netherFungus) {
        if (netherFungus && isNetherFungusLeaf(leafBlock.getType())) {
            return isConnectedToRemovedLog(leafBlock, removedLogs, new HashSet<>(), 0);
        }

        String mode = config.getLeafRemovalMode().toLowerCase();

        switch (mode) {
            case "aggressive":
                // Remove ALL leaves within radius - most thorough
                return true;

            case "radius":
                // Remove leaves that don't have logs within 4 blocks (simple distance check)
                return !hasNearbyActiveLog(leafBlock.getLocation(), config, removedLogs, 4);

            case "smart":
            default:
                // Smart detection with pathfinding (current approach)
                Location leafLoc = leafBlock.getLocation();

                // Strategy 1: Check for nearby logs that weren't removed
                if (!hasNearbyActiveLog(leafLoc, config, removedLogs, 6)) {
                    return true; // No active logs nearby - definitely orphaned
                }

                // Strategy 2: Advanced - check if leaf is connected to remaining logs via other leaves
                if (!isConnectedToActiveLog(leafBlock, config, removedLogs, new HashSet<>(), 0)) {
                    return true; // Not connected to any remaining logs
                }

                return false; // Connected to remaining logs - keep it
        }
    }

    // Updated helper method with configurable radius:
    private static boolean hasNearbyActiveLog(Location leafLoc, Config config, Set<Location> removedLogs, int checkRadius) {
        for (int x = -checkRadius; x <= checkRadius; x++) {
            for (int y = -checkRadius; y <= checkRadius; y++) {
                for (int z = -checkRadius; z <= checkRadius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Skip the leaf itself

                    Location checkLoc = leafLoc.clone().add(x, y, z);
                    Block checkBlock = checkLoc.getBlock();

                    // If there's a log here AND it wasn't removed in this session
                    if (TreeChopUtils.isLog(checkBlock.getType(), config) &&
                            !removedLogs.contains(checkLoc)) {
                        return true; // Found an active log nearby
                    }
                }
            }
        }

        return false; // No active logs found
    }

    private static boolean isConnectedToActiveLog(Block startBlock, Config config, Set<Location> removedLogs, Set<Location> visited, int depth) {
        // Prevent infinite recursion and limit search depth for performance
        if (depth > 8 || visited.size() > 100) {
            return false;
        }

        Location startLoc = startBlock.getLocation();

        // Already visited this location
        if (visited.contains(startLoc)) {
            return false;
        }
        visited.add(startLoc);

        // Check immediate surroundings (6 directions + diagonals)
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Location checkLoc = startLoc.clone().add(x, y, z);
                    Block checkBlock = checkLoc.getBlock();
                    Material checkType = checkBlock.getType();

                    // Found an active log - connected!
                    if (TreeChopUtils.isLog(checkType, config) && !removedLogs.contains(checkLoc)) {
                        return true;
                    }

                    // Found another leaf - continue searching through it
                    if (isLeafBlock(checkType, config) && !visited.contains(checkLoc)) {
                        if (isConnectedToActiveLog(checkBlock, config, removedLogs, visited, depth + 1)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false; // No connection to active logs found
    }

    private static boolean isConnectedToRemovedLog(Block startBlock, Set<Location> removedLogs, Set<Location> visited, int depth) {
        if (removedLogs == null || removedLogs.isEmpty()) {
            return true;
        }
        if (depth > 12 || visited.size() > 200) {
            return false;
        }

        Location startLoc = startBlock.getLocation();
        if (!visited.add(startLoc)) {
            return false;
        }
        if (removedLogs.contains(startLoc)) {
            return true;
        }

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    Location checkLoc = startLoc.clone().add(x, y, z);
                    if (removedLogs.contains(checkLoc)) {
                        return true;
                    }

                    Block neighbourBlock = checkLoc.getBlock();
                    Material neighbourType = neighbourBlock.getType();
                    if (isNetherFungusLeaf(neighbourType) || NETHER_FUNGUS_LOGS.contains(neighbourType)) {
                        if (isConnectedToRemovedLog(neighbourBlock, removedLogs, visited, depth + 1)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }


    public static boolean isLeafBlock(Material material, Config config) {
        return config.getLeafTypes().contains(material);
    }

    private static boolean isNetherFungusLeaf(Material material) {
        return NETHER_FUNGUS_LEAVES.contains(material);
    }

    private static boolean isNetherFungusLog(Material material) {
        return material != null && NETHER_FUNGUS_LOGS.contains(material);
    }

    private static final EnumSet<Material> NETHER_FUNGUS_LEAVES = EnumSet.of(
            Material.NETHER_WART_BLOCK,
            Material.WARPED_WART_BLOCK,
            Material.SHROOMLIGHT
    );

    private static final EnumSet<Material> NETHER_FUNGUS_LOGS = EnumSet.of(
            Material.CRIMSON_STEM,
            Material.STRIPPED_CRIMSON_STEM,
            Material.CRIMSON_HYPHAE,
            Material.STRIPPED_CRIMSON_HYPHAE,
            Material.WARPED_STEM,
            Material.STRIPPED_WARPED_STEM,
            Material.WARPED_HYPHAE,
            Material.STRIPPED_WARPED_HYPHAE
    );
}









