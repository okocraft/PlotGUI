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
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import net.okocraft.plotgui.PlotGUI;

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

    public void addClaim(String claim, World world, Location sign, BlockFace signDirection, OfflinePlayer owner) {
        ProtectedRegion region = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world)).getRegion(claim);
        if (region == null) {
            return;
        }

        get().set(claim + ".world", world.getName());
        get().set(claim + ".sign.x", sign.getBlockX());
        get().set(claim + ".sign.y", sign.getBlockY());
        get().set(claim + ".sign.z", sign.getBlockZ());
        get().set(claim + ".sign.facing", signDirection.name());
        if (owner != null) {
            get().set(claim + ".owner", owner.getUniqueId().toString());
        }
        get().set(claim + ".is-wallsign", getSignLocation(claim).getBlock().getBlockData() instanceof WallSign);

        save();

        region.getMembers().addAll(region.getOwners());
        region.getOwners().clear();
    }

    public boolean removeClaim(String claim) {
        if (!get().isConfigurationSection(claim)) {
            return false;
        }
        get().set(claim, null);
        save();
        return true;
    }

    public String getWorldName(String claim) {
        return get().getString(claim + ".world", "");
    }

    public Location getSignLocation(String claim) {
        World world = Bukkit.getWorld(getWorldName(claim));
        if (world == null) {
            PLUGIN.getLogger().warning("The world " + getWorldName(claim) + " does not exist.");
            return null;
        }

        if (!get().isInt(claim + ".sign.x")) {
            PLUGIN.getLogger().warning("Sign coordinate x is not set for " + claim + ".");
            return null;
        } else if (!get().isInt(claim + ".sign.y")) {
            PLUGIN.getLogger().warning("Sign coordinate y is not set for " + claim + ".");
            return null;
        } else if (!get().isInt(claim + ".sign.z")) {
            PLUGIN.getLogger().warning("Sign coordinate z is not set for " + claim + ".");
            return null;
        }

        int x = get().getInt(claim + ".sign.x");
        int y = get().getInt(claim + ".sign.y");
        int z = get().getInt(claim + ".sign.z");

        return new Location(world, x, y, z);
    }

    public boolean isWallSign(String claim) {
        return get().getBoolean(claim + ".is-wallsign");
    }

    public BlockFace getSignFacing(String claim) {
        try {
            return BlockFace.valueOf(get().getString(claim + ".sign.facing"));
        } catch (IllegalArgumentException e) {
            return BlockFace.NORTH;
        }
    }

    public OfflinePlayer getOwner(String claim) {
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(get().getString(claim + ".owner", "")));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setOwner(String claim, OfflinePlayer owner) {
        addMember(claim, getOwner(claim));
        get().set(claim + ".owner", owner.getUniqueId().toString());
        save();
        placeSign(claim);
    }

    public void removeOwner(String claim, OfflinePlayer owner) {
            addMember(claim, owner);
        get().set(claim + ".owner", null);
        save();
        placeSign(claim);
    }

    public Set<OfflinePlayer> getMembers(String claim) {
        ProtectedRegion region = getRegion(claim);
        if (region == null) {
            return Set.of();
        }

        return region.getMembers().getUniqueIds().stream().map(Bukkit::getOfflinePlayer).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public boolean addMember(String claim, OfflinePlayer player) {
        ProtectedRegion region = getRegion(claim);
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

    public boolean removeMember(String claim, OfflinePlayer player) {
        ProtectedRegion region = getRegion(claim);
        if (region == null || player == null) {
            return false;
        }

        region.getMembers().removePlayer(player.getUniqueId());
        return true;
    }

    public ProtectedRegion getRegion(String claim) {
        if (claim == null) {
            return null;
        }

        Location signLocation = getSignLocation(claim);
        if (signLocation == null) {
            return null;
        }

        return WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(signLocation.getWorld())).getRegion(claim);
    }

    public Set<String> getClaims() {
        return get().getKeys(false).stream().filter(claim -> getSignLocation(claim) != null)
                .filter(claim -> getSignFacing(claim) != null).collect(Collectors.toSet());
    }

    public boolean hasClaim(OfflinePlayer player) {
        Set<String> claims = getClaims();
        if (claims.size() > 100) {
            return getClaims().parallelStream().map(this::getOwner).filter(Objects::nonNull).anyMatch(player::equals);
        } else {
            return getClaims().stream().map(this::getOwner).filter(Objects::nonNull).anyMatch(player::equals);
        }
    }

    public String getClaimBySignLocation(Location signLocation) {
        for (String claim : getClaims()) {
            Location currentLocation = getSignLocation(claim);
            if (currentLocation.getBlockX() == signLocation.getBlockX()
                    && currentLocation.getBlockY() == signLocation.getBlockY()
                    && currentLocation.getBlockZ() == signLocation.getBlockZ()) {
                return claim;
            }
        }

        return null;
    }

    public void placeSign(String claim) {
        Location signLocation = getSignLocation(claim);
        if (signLocation == null) {
            return;
        }

        BlockFace facing = getSignFacing(claim);
        Block signBlock = getSignLocation(claim).getBlock();
        if (isWallSign(claim)) {
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
        sign.setLine(1, claim);
        OfflinePlayer owner = getOwner(claim);
        String line2 = owner == null ? Messages.getInstance().getMessage("other.click-here-to-claim")
                : Optional.ofNullable(owner.getName()).orElse(owner.getUniqueId().toString());
        sign.setLine(2, line2);
        sign.update();
    }

    public void regen(String claim, CommandSender executor) {
        long startTime = System.currentTimeMillis();
        long cooldown = regenCooldown.getOrDefault(claim, 0L) + 1000 * 60 * 60 - startTime;
        if (cooldown > 0) {
            Messages.getInstance().sendMessage(executor, "gui.regen-cooldown",
                    Map.of("%cooldown%", String.valueOf(cooldown / 1000)));
            return;
        }
        regenCooldown.put(claim, startTime);

        regenMultiRegions(new HashSet<String>() {
            private static final long serialVersionUID = 1L;
            {
                add(claim);
            }
        }, executor);
    }

    public void regenMultiRegions(Set<String> claims, CommandSender executor) {
        String claim;
        try {
            claim = claims.iterator().next();
            claims.remove(claim);
        } catch (NoSuchElementException e) {
            return;
        }

        Location signLocation = getSignLocation(claim);
        if (signLocation == null) {
            return;
        }

        ProtectedRegion region = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(signLocation.getWorld())).getRegion(claim);
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

                Plots.getInstance().placeSign(claim);

                cancel();
                PLUGIN.getLogger().info("Plot regen operation on " + claim + " finished in "
                        + (System.currentTimeMillis() - startTime) + " ms.");
                Messages.getInstance().sendMessage(executor, "gui.regen-finish",
                        Map.of("%time%", String.valueOf((System.currentTimeMillis() - startTime) / 1000)));
                regenMultiRegions(claims, executor);
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
    public Set<String> getInactiveClaims(int threshold) {
        Set<String> result = new HashSet<>();
        getClaims().forEach(claim -> {
            OfflinePlayer owner = getOwner(claim);
            if (owner == null || !owner.hasPlayedBefore()) {
                return;
            }
            long lastPlayed = owner.getLastPlayed();
            int noLoginTerm = (int) (System.currentTimeMillis() - lastPlayed) / (1000 * 60 * 60 * 24);
            if (noLoginTerm > threshold) {
                result.add(claim);
            }
        });

        return result;
    }
}