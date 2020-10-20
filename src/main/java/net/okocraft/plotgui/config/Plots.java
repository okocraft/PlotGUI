package net.okocraft.plotgui.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.okocraft.plotgui.PlotGUI;

public final class Plots extends CustomConfig {

    private static final Plots INSTANCE = new Plots(JavaPlugin.getPlugin(PlotGUI.class));
    
    private final PlotGUI plugin;

    private final Map<String, Long> regenCooldown = new HashMap<>();

    private Plots(PlotGUI plugin) {
        super(plugin, "plots.yml");
        this. plugin = plugin;
    }

    public static Plots getInstance() {
        return INSTANCE;
    }

    public boolean removePlot(String plotName) {
        if (!get().isConfigurationSection(plotName)) {
            return false;
        }
        get().set(plotName, null);
        save();
        return true;
    }

    public String getWorldName(String plotName) {
        return get().getString(plotName + ".world", "");
    }

    public Location getSignLocation(String plotName) {
        World world = Bukkit.getWorld(getWorldName(plotName));
        if (world == null) {
            plugin.getLogger().warning("The world " + getWorldName(plotName) + " does not exist.");
            return null;
        }

        if (!get().isInt(plotName + ".sign.x")) {
            plugin.getLogger().warning("Sign coordinate x is not set for " + plotName + ".");
            return null;
        } else if (!get().isInt(plotName + ".sign.y")) {
            plugin.getLogger().warning("Sign coordinate y is not set for " + plotName + ".");
            return null;
        } else if (!get().isInt(plotName + ".sign.z")) {
            plugin.getLogger().warning("Sign coordinate z is not set for " + plotName + ".");
            return null;
        }

        int x = get().getInt(plotName + ".sign.x");
        int y = get().getInt(plotName + ".sign.y");
        int z = get().getInt(plotName + ".sign.z");

        return new Location(world, x, y, z);
    }

    public boolean isWallSign(String plotName) {
        return get().getBoolean(plotName + ".is-wallsign");
    }

    public BlockFace getSignFacing(String plotName) {
        try {
            return BlockFace.valueOf(get().getString(plotName + ".sign.facing"));
        } catch (IllegalArgumentException e) {
            return BlockFace.NORTH;
        }
    }

    public Set<OfflinePlayer> getOwners(String plotName) {
        ProtectedRegion region = getRegion(plotName);
        if (region == null) {
            return Set.of();
        }

        return region.getOwners().getUniqueIds().stream().map(Bukkit::getOfflinePlayer).collect(Collectors.toSet());
    }

    public boolean addOwner(String plotName, OfflinePlayer player) {
        ProtectedRegion region = getRegion(plotName);
        if (region == null || player == null) {
            return false;
        }

        LocalPlayer localPlayer;
        try {
            localPlayer = WorldGuardPlugin.inst().wrapOfflinePlayer(player);
            if (region.getOwners().contains(localPlayer)) {
                return false;
            }
            region.getOwners().addPlayer(localPlayer);
        } catch (NullPointerException e) {
            if (region.getOwners().contains(player.getUniqueId())) {
                return false;
            }
            region.getOwners().addPlayer(player.getUniqueId());
        }

        return true;
    }

    public boolean removeOwner(String plotName, OfflinePlayer player) {
        ProtectedRegion region = getRegion(plotName);
        if (region == null || player == null) {
            return false;
        }

        region.getOwners().removePlayer(player.getUniqueId());
        return true;
    }

    public Set<OfflinePlayer> getMembers(String plotName) {
        ProtectedRegion region = getRegion(plotName);
        if (region == null) {
            return Set.of();
        }

        return region.getMembers().getUniqueIds().stream().map(Bukkit::getOfflinePlayer).collect(Collectors.toSet());
    }

    public boolean addMember(String plotName, OfflinePlayer player) {
        ProtectedRegion region = getRegion(plotName);
        if (region == null || player == null) {
            return false;
        }

        LocalPlayer localPlayer;
        try {
            localPlayer = WorldGuardPlugin.inst().wrapOfflinePlayer(player);
            if (region.getMembers().contains(localPlayer)) {
                return false;
            }
            region.getMembers().addPlayer(localPlayer);
        } catch (NullPointerException e) {
            if (region.getMembers().contains(player.getUniqueId())) {
                return false;
            }
            region.getMembers().addPlayer(player.getUniqueId());
        }

        return true;
    }

    public boolean removeMember(String plotName, OfflinePlayer player) {
        ProtectedRegion region = getRegion(plotName);
        if (region == null || player == null) {
            return false;
        }

        region.getMembers().removePlayer(player.getUniqueId());
        return true;
    }

    public ProtectedRegion getRegion(String plotName) {
        if (plotName == null) {
            return null;
        }

        Location signLocation = getSignLocation(plotName);
        if (signLocation == null) {
            return null;
        }

        return WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(signLocation.getWorld())).getRegion(plotName);
    }

    public Set<String> getPlots() {
        return get().getKeys(false).stream().filter(plotName -> getSignLocation(plotName) != null)
                .filter(plotName -> getSignFacing(plotName) != null).collect(Collectors.toSet());
    }

    public Set<String> getPlots(OfflinePlayer player) {
        Set<String> plots = getPlots();
        plots.removeIf(plotName -> !getOwners(plotName).contains(player));
        return plots;
    }

    public boolean hasPlot(OfflinePlayer player) {
        return !getPlots(player).isEmpty();
    }

    public String getPlotBySignLocation(Location signLocation) {
        for (String plotName : getPlots()) {
            Location currentLocation = getSignLocation(plotName);
            if (currentLocation.getWorld().equals(signLocation.getWorld())
                    && currentLocation.getBlockX() == signLocation.getBlockX()
                    && currentLocation.getBlockY() == signLocation.getBlockY()
                    && currentLocation.getBlockZ() == signLocation.getBlockZ()) {
                return plotName;
            }
        }

        return null;
    }

    public void placeSign(String plotName) {
        Location signLocation = getSignLocation(plotName);
        if (signLocation == null) {
            return;
        }

        BlockFace facing = getSignFacing(plotName);
        Block signBlock = getSignLocation(plotName).getBlock();
        if (isWallSign(plotName)) {
            signBlock.getRelative(facing.getOppositeFace()).setType(Material.BEDROCK);
            signBlock.setType(Material.OAK_WALL_SIGN);
            Directional data = (Directional) signBlock.getBlockData();
            data.setFacing(facing);
            signBlock.setBlockData(data);
        } else {
            signBlock.getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
            signBlock.setType(Material.OAK_SIGN);
            Rotatable data = (Rotatable) signBlock.getBlockData();
            data.setRotation(facing);
            signBlock.setBlockData(data);
        }

        Sign sign = (Sign) signBlock.getState();
        sign.setLine(0, "[PlotGUI]");
        sign.setLine(1, plotName);
        Set<OfflinePlayer> owners = getOwners(plotName);
        OfflinePlayer owner = owners.isEmpty() ? null : owners.iterator().next();
        String line2 = owner == null ? plugin.messages.getMessage("other.click-here-to-claim")
                : Optional.ofNullable(owner.getName()).orElse(owner.getUniqueId().toString());
        sign.setLine(2, line2);
        sign.update();
    }

    public boolean regen(String plotName, CommandSender executor) {
        long startTime = System.currentTimeMillis();
        long cooldown = regenCooldown.getOrDefault(plotName, 0L) + 1000 * plugin.config.getRegenCooldown()
                - startTime;
        if (cooldown > 0) {
            plugin.messages.sendMessage(executor, "gui.regen-cooldown",
                    Map.of("%cooldown%", String.valueOf(cooldown / 1000)));
            return false;
        }
        regenCooldown.put(plotName, startTime);

        regenMultiRegions(new HashSet<String>() {
            private static final long serialVersionUID = 1L;
            {
                add(plotName);
            }
        }, executor);
        return true;
    }

    public void regenMultiRegions(Set<String> plotNames, CommandSender executor) {
        String plotName;
        try {
            plotName = plotNames.iterator().next();
            plotNames.remove(plotName);
        } catch (NoSuchElementException e) {
            return;
        }

        Location signLocation = getSignLocation(plotName);
        if (signLocation == null) {
            return;
        }

        ProtectedRegion region = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(signLocation.getWorld())).getRegion(plotName);
        if (region == null) {
            return;
        }

        new BukkitRunnable() {
            final long startTime = System.currentTimeMillis();
            final int floorHeight = signLocation.getBlockY() - 1;
            int minX = region.getMinimumPoint().getBlockX();
            int minY = region.getMinimumPoint().getBlockY();
            int minZ = region.getMinimumPoint().getBlockZ();
            int x = minX;
            int y = minY;
            int z = minZ;
            int maxX = region.getMaximumPoint().getBlockX();
            int maxY = region.getMaximumPoint().getBlockY();
            int maxZ = region.getMaximumPoint().getBlockZ();
            final Location currentLocation = new Location(signLocation.getWorld(), x, y, z);
            Chunk currentChunk = currentLocation.getBlock().getChunk();

            @Override
            public void run() {

                long blockChanges = 0;
                while (x <= maxX) {
                    while (y <= maxY) {
                        while (z <= maxZ) {
                            if (!currentLocation.getBlock().getChunk().equals(currentChunk)) {
                                currentChunk.setForceLoaded(false);
                                currentChunk = currentLocation.getBlock().getChunk();
                            }

                            if (!currentChunk.isForceLoaded()) {
                                currentChunk.setForceLoaded(true);
                            }

                            for (Entity entity : currentChunk.getEntities()) {
                                if (entity.getLocation().getBlockX() == x && entity.getLocation().getBlockY() == y
                                        && entity.getLocation().getBlockZ() == z && !(entity instanceof HumanEntity)) {
                                    entity.remove();
                                }
                            }

                            regenPosition(currentLocation, floorHeight);
                            blockChanges++;
                            z++;

                            currentLocation.setZ(z);
                            if (blockChanges >= plugin.config.getRegenBlocksPerTickUnit()) {
                                return;
                            }
                        }
                        z = region.getMinimumPoint().getBlockZ();
                        y++;
                        currentLocation.setZ(z);
                        currentLocation.setY(y);
                    }
                    y = region.getMinimumPoint().getBlockY();
                    x++;
                    currentLocation.setY(y);
                    currentLocation.setX(x);
                }

                Plots.getInstance().placeSign(plotName);

                cancel();
                plugin.getLogger().info("Plot regen operation on " + plotName + " finished in "
                        + (System.currentTimeMillis() - startTime) + " ms.");
                plugin.messages.sendMessage(executor, "gui.regen-finish",
                        Map.of("%time%", String.valueOf((System.currentTimeMillis() - startTime) / 1000)));
                regenMultiRegions(plotNames, executor);
            }
        }.runTaskTimer(plugin, 0L, plugin.config.getRegenTickUnit());
    }

    private void regenPosition(Location location, int floor) {
        Block block = location.getBlock();
        if (block.getState() instanceof Container) {
            ((Container) block.getState()).getInventory().clear();
        }

        if (block.getType() == Material.BEDROCK) {
            return;

        } else if (location.getBlockY() > floor) {
            block.setType(Material.AIR);

        } else if (location.getBlockY() == floor) {
            block.setType(Material.GRASS_BLOCK);

        } else if (location.getBlockY() >= floor - 5) {
            block.setType(Material.DIRT);

        } else {
            block.setType(Material.STONE);
        }
    }

    /**
     * オーナーが指定した{@code threshold} 日以上ログインしていない区画保護をすべて取得する。
     * 
     * @param threshold 日数。
     * @return アクティブでない区画保護。
     */
    public Set<String> getInactivePlots(int threshold) {
        Set<String> result = new HashSet<>();
        getPlots().forEach(plotName -> {
            Set<OfflinePlayer> owners = getOwners(plotName);
            boolean isActive = owners.isEmpty()
                    || owners.stream().filter(owner -> owner != null && owner.hasPlayedBefore()).filter(owner -> {
                        long lastPlayed = owner.getLastPlayed();
                        long cur = System.currentTimeMillis();
                        int noLoginTerm = (int) ((cur - lastPlayed) / (1000 * 60 * 60 * 24));
                        return noLoginTerm <= threshold;
                    }).findAny().isPresent();

            if (!isActive) {
                result.add(plotName);
            }
        });

        return result;
    }

    public void purgeInactivePlots(int threshold) {
        Set<String> inactivePlots = getInactivePlots(threshold);
        plugin.getLogger().info(inactivePlots.size() + " region" + (inactivePlots.size() > 1 ? "s" : "")
                + " is marked as inactive." + (inactivePlots.size() > 0 ? "Then starting regeneration." : ""));
        inactivePlots.forEach(plotName -> {
            Set<OfflinePlayer> owners = getOwners(plotName);
            logPurge(plotName, owners);
            owners.forEach(owner -> removeOwner(plotName, owner));
            getMembers(plotName).forEach(owner -> removeMember(plotName, owner));
        });
        regenMultiRegions(inactivePlots, Bukkit.getConsoleSender());
    }

    private void logPurge(String plotName, Set<OfflinePlayer> owners) {
        plugin.getLogger().info("Owner(s) of " + plotName + ":");

        StringBuilder sb = new StringBuilder();
        String firstOwner = null;
        for (OfflinePlayer owner : getOwners(plotName)) {
            if (firstOwner == null) {
                firstOwner = owner.getName();
            }
            sb.append("  ").append(owner.getName() == null ? owner.getUniqueId().toString() : owner.getName())
                    .append(" (last login = ").append(Instant.ofEpochMilli(owner.getLastPlayed())).append(")\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        plugin.getLogger().info(sb.toString());

        try {
            Path logFolder = plugin.getDataFolder().toPath().resolve("plot-removal-log");
            if (!Files.exists(logFolder) || !Files.isDirectory(logFolder)) {
                Files.createDirectories(logFolder);
            }

            Path logFile = logFolder.resolve(plotName + "(" + firstOwner + ")" + ".log");
            if (Files.exists(logFile)) {
                Files.delete(logFile);
            }
            Files.createFile(logFile);

            Files.write(logFile, sb.toString().getBytes());

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Cannot make log file of plot deletion because of file I/O Error.", e);
        }
    }
}