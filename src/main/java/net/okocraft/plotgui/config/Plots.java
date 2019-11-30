package net.okocraft.plotgui.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import net.okocraft.plotgui.PlotGUI;
import net.okocraft.plotgui.Utility;

public final class Plots extends CustomConfig {

    private static final PlotGUI PLUGIN = PlotGUI.getInstance();
    private static final Plots INSTANCE = new Plots();

    private final Map<String, Long> regenCooldown = new HashMap<>();

    private Plots() {
        super("plots.yml");
    }

    public static Plots getInstance() {
        return INSTANCE;
    }

    public void addPlot(String plotName, World world, Location sign, BlockFace signDirection, OfflinePlayer owner) {
        ProtectedRegion region = Utility.getRegion(world, plotName);
        if (region == null) {
            return;
        }

        get().set(plotName + ".world", world.getName());
        get().set(plotName + ".sign.x", sign.getBlockX());
        get().set(plotName + ".sign.y", sign.getBlockY());
        get().set(plotName + ".sign.z", sign.getBlockZ());
        get().set(plotName + ".sign.facing", signDirection.name());
        if (owner != null) {
            get().set(plotName + ".owner", owner.getUniqueId().toString());
        }
        get().set(plotName + ".is-wallsign", Utility.isWallSign(getSignLocation(plotName).getBlock()));

        save();

        region.getMembers().addAll(region.getOwners());
        region.getOwners().clear();
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
            PLUGIN.getLogger().warning("The world " + getWorldName(plotName) + " does not exist.");
            return null;
        }

        if (!get().isInt(plotName + ".sign.x")) {
            PLUGIN.getLogger().warning("Sign coordinate x is not set for " + plotName + ".");
            return null;
        } else if (!get().isInt(plotName + ".sign.y")) {
            PLUGIN.getLogger().warning("Sign coordinate y is not set for " + plotName + ".");
            return null;
        } else if (!get().isInt(plotName + ".sign.z")) {
            PLUGIN.getLogger().warning("Sign coordinate z is not set for " + plotName + ".");
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

    public OfflinePlayer getOwner(String plotName) {
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(get().getString(plotName + ".owner", "")));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setOwner(String plotName, OfflinePlayer owner) {
        addMember(plotName, getOwner(plotName));
        get().set(plotName + ".owner", owner.getUniqueId().toString());
        save();
        placeSign(plotName);
    }

    public void removeOwner(String plotName) {
        OfflinePlayer owner = getOwner(plotName);
        if (owner != null) {
            addMember(plotName, owner);
        }
        get().set(plotName + ".owner", null);
        save();
        placeSign(plotName);
    }

    public Set<OfflinePlayer> getMembers(String plotName) {
        ProtectedRegion region = getRegion(plotName);
        if (region == null) {
            return Set.of();
        }

        return region.getMembers().getUniqueIds().stream().map(Bukkit::getOfflinePlayer).filter(Objects::nonNull)
                .collect(Collectors.toSet());
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

    public boolean hasPlot(OfflinePlayer player) {
        Set<String> plots = getPlots();
        if (plots.size() > 100) {
            return getPlots().parallelStream().map(this::getOwner).filter(Objects::nonNull).anyMatch(player::equals);
        } else {
            return getPlots().stream().map(this::getOwner).filter(Objects::nonNull).anyMatch(player::equals);
        }
    }

    public String getPlotBySignLocation(Location signLocation) {
        for (String plotName : getPlots()) {
            Location currentLocation = getSignLocation(plotName);
            if (currentLocation.getBlockX() == signLocation.getBlockX()
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
        OfflinePlayer owner = getOwner(plotName);
        String line2 = owner == null ? Messages.getInstance().getMessage("other.click-here-to-claim")
                : Optional.ofNullable(owner.getName()).orElse(owner.getUniqueId().toString());
        sign.setLine(2, line2);
        sign.update();
    }

    public boolean regen(String plotName, CommandSender executor) {
        long startTime = System.currentTimeMillis();
        long cooldown = regenCooldown.getOrDefault(plotName, 0L) + 1000 * Config.getInstance().getRegenCooldown() - startTime;
        if (cooldown > 0) {
            Messages.getInstance().sendMessage(executor, "gui.regen-cooldown",
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
            int x = region.getMinimumPoint().getBlockX();
            int y = region.getMinimumPoint().getBlockY();
            int z = region.getMinimumPoint().getBlockZ();
            int maxX = region.getMaximumPoint().getBlockX();
            int maxY = region.getMaximumPoint().getBlockY();
            int maxZ = region.getMaximumPoint().getBlockZ();
            final Location currentLocation = new Location(signLocation.getWorld(), x, y, z);

            @Override
            public void run() {

                long blockChanges = 0;
                while (x <= maxX) {
                    while (y <= maxY) {
                        while (z <= maxZ) {
                            regenPosition(currentLocation, floorHeight);
                            blockChanges++;
                            z++;

                            currentLocation.setZ(z);
                            if (blockChanges >= Config.getInstance().getRegenBlocksPerTickUnit()) {
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
                PLUGIN.getLogger().info("Plot regen operation on " + plotName + " finished in "
                        + (System.currentTimeMillis() - startTime) + " ms.");
                Messages.getInstance().sendMessage(executor, "gui.regen-finish",
                        Map.of("%time%", String.valueOf((System.currentTimeMillis() - startTime) / 1000)));
                regenMultiRegions(plotNames, executor);
            }
        }.runTaskTimer(PLUGIN, 0L, Config.getInstance().getRegenTickUnit());
    }

    private void regenPosition(Location location, int floor) {
        Block block = location.getBlock();
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
            OfflinePlayer owner = getOwner(plotName);
            if (owner == null || !owner.hasPlayedBefore()) {
                return;
            }
            long lastPlayed = owner.getLastPlayed();
            int noLoginTerm = (int) (System.currentTimeMillis() - lastPlayed) / (1000 * 60 * 60 * 24);
            if (noLoginTerm > threshold) {
                result.add(plotName);
            }
        });

        return result;
    }
}