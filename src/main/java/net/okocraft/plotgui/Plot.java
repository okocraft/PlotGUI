package net.okocraft.plotgui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.scheduler.BukkitRunnable;

import lombok.Getter;
import net.okocraft.plotgui.config.Messages;
import net.okocraft.plotgui.event.PlotRegenCompleteEvent;

public class Plot {
    
    private final PlotGUI plugin;

    @Getter
    private ProtectedRegion region;
    
    private final YamlConfiguration serializer = new YamlConfiguration();
    
    private long previousRegenTime = 0;

    private void checkPlotFlag() throws IllegalStateException {
        if (!isPlot(region)) {
            throw new IllegalStateException("This region is no longer plot: " + region.getId());
        }
    }
    
    private Plot(PlotGUI plugin, ProtectedRegion region) throws IllegalStateException {
        if (!isPlot(region)) {
            throw new IllegalArgumentException("The region " + region.getId() + " do not have plot flag.");
        }
        this.plugin = plugin;
        this.region = region;

        setKeepTerm(System.currentTimeMillis());
        setRegenHeight(getAverageHeight(5, getWorld(), region));
    }

    @SuppressWarnings("unchecked")
    private <T> T getData(String dataPath, T def) throws IllegalStateException {
        checkPlotFlag();

        try {
            String flagValue = region.getFlag(PlotFlag.get());
            serializer.loadFromString(flagValue != null ? flagValue : "");
        } catch (InvalidConfigurationException e) {
            try {
                serializer.loadFromString("");
            } catch (InvalidConfigurationException ignored) {
            }
            region.setFlag(PlotFlag.get(), serializer.saveToString());
        }

        Object obj = serializer.get(dataPath);
        if (obj != null) {
            try {
                return (T) obj;
            } catch (ClassCastException ignored) {
            }
        }

        serializer.set(dataPath, def);
        region.setFlag(PlotFlag.get(), serializer.saveToString());
        return def;
    }

    private <T> void setData(String dataPath, T value) throws IllegalStateException {
        checkPlotFlag();

        try {
            String flagValue = region.getFlag(PlotFlag.get());
            serializer.loadFromString(flagValue != null ? flagValue : "");
            serializer.set(dataPath, value);
            region.setFlag(PlotFlag.get(), serializer.saveToString());
        } catch (InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING, e, () -> "Stored configuration in flag is invalid.");
        }
    }

    public World getWorld() {
        RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
        for (World world : plugin.getServer().getWorlds()) {
            if (rc.get(BukkitAdapter.adapt(world)).getRegions().containsValue(region)) {
                return world;
            }
        }

        throw new IllegalStateException("This Plot's region does not belong to any world. Region may removed by command or other plugin.");
    }
    
    public int getRegenHeight() throws IllegalStateException {
        return getData("regen-height", getAverageHeight(5, getWorld(), region));
    }

    public void setRegenHeight(int regenHeight) throws IllegalStateException {
        setData("regen-height", regenHeight);
    }

    public long getKeepTerm() throws IllegalStateException {
        return getData("keep-term", System.currentTimeMillis() + plugin.config.getPlotPurgeDays() + 24 * 60 * 60 * 1000);
    }

    public void setKeepTerm(long keepTerm) throws IllegalStateException {
        setData("keep-term", keepTerm);
    }

    public UUID getPlotOwnerUid() throws IllegalStateException {
        try {
            return UUID.fromString(getData("plot-onwer-uid", ""));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setPlotOwnerUid(UUID plotOwnerUid) throws IllegalStateException {
        setData("plot-onwer-uid", plotOwnerUid == null ? null : plotOwnerUid.toString());
    }

    private static int getAverageHeight(int precision, World world, ProtectedRegion region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int dx = (max.getBlockX() - min.getBlockX()) / precision;
        int dz = (max.getBlockZ() - min.getBlockZ()) / precision;
        int heightSum = 0;
        for (int x = 0; x < precision; x++) {
            for (int z = 0; z < precision; z++) {
                heightSum += world.getHighestBlockYAt(x * dx + min.getBlockX(), z * dz + min.getBlockZ());
            }
        }
        return heightSum / (precision * precision);
    }

    public long getCooldown() {
        checkPlotFlag();
        return this.previousRegenTime + 1000 * plugin.config.getRegenCooldown() - System.currentTimeMillis();
    }

    public void regen(CommandSender executor, UUID oldOwner) throws IllegalStateException {
        checkPlotFlag();
        long startTime = this.previousRegenTime = System.currentTimeMillis();

        Plot plotInstance = this;
        new BukkitRunnable(){
            World world = getWorld();
            final int minX = region.getMinimumPoint().getBlockX();
            final int minY = region.getMinimumPoint().getBlockY();
            final int minZ = region.getMinimumPoint().getBlockZ();
            int x = minX;
            int y = minY;
            int z = minZ;
            final int maxX = region.getMaximumPoint().getBlockX();
            final int maxY = region.getMaximumPoint().getBlockY();
            final int maxZ = region.getMaximumPoint().getBlockZ();
            Block currentBlock;
            Chunk currentChunk;

            @Override
            public void run() {

                long blockChanges = 0;
                while (x <= maxX) {
                    while (y <= maxY) {
                        while (z <= maxZ) {

                            // 再生成する予定のチャンクと再生成するブロックがずれていたら修正
                            currentBlock = world.getBlockAt(x, y, z);
                            if (!currentBlock.getChunk().equals(currentChunk)) {
                                if (currentChunk != null) {
                                    currentChunk.setForceLoaded(false);
                                }
                                currentChunk = currentBlock.getChunk();
                                currentChunk.setForceLoaded(true);

                                // 再生成するチャンクに居るエンティティを全部削除する。
                                for (Entity entity : currentChunk.getEntities()) {
                                    if (!(entity instanceof HumanEntity)) {
                                        entity.remove();
                                    }
                                }
                            }


                            regenBlock(currentBlock, getRegenHeight());
                            blockChanges++;
                            z++;

                            if (blockChanges >= plugin.config.getRegenBlocksPerTickUnit()) {
                                return;
                            }
                        }
                        z = minZ;
                        y++;
                    }
                    y = minY;
                    x++;
                }

                currentChunk.setForceLoaded(false);
                cancel();
                plugin.getServer().getPluginManager()
                        .callEvent(new PlotRegenCompleteEvent(plotInstance, System.currentTimeMillis() - startTime, executor, oldOwner));
            }
        }.runTaskTimer(plugin, 0, plugin.config.getRegenTickUnit());
    }

    private void regenBlock(Block block, int groundHeight) {
        if (block.getState() instanceof Container) {
            ((Container) block.getState()).getInventory().clear();
        }

        if (SignUtil.isPlotGUISign(SignUtil.getSignFrom(block))) {
            return;
        } else if (block.getType() == Material.BEDROCK) {
            return;

        } else if (block.getY() > groundHeight) {
            block.setType(Material.AIR);

        } else if (block.getY() == groundHeight) {
            block.setType(Material.GRASS_BLOCK);

        } else if (block.getY() >= groundHeight - 5) {
            block.setType(Material.DIRT);

        } else {
            block.setType(Material.STONE);
        }
    }

    /**
     * このプロットがアクティブでないかどうかを調べる。
     * 
     * @param threshold 日数。
     * @param isEmptyOwnersInactive オーナーが誰もいない場合に非アクティブと判定するかどうか。
     * @return 保護のオーナー全員＋区画のオーナーのそれぞれが、指定した{@code threshold} 日以上ログインしていない場合true
     */
    public boolean isInactive(int threshold, boolean isEmptyOwnerInactive) throws IllegalStateException {
        checkPlotFlag();
        Set<UUID> owners = new HashSet<>(region.getOwners().getUniqueIds());
        Optional.ofNullable(getPlotOwnerUid()).ifPresent(owners::add);
        if (isEmptyOwnerInactive && owners.isEmpty()) {
            return true;
        }

        for (UUID ownerUid : owners) {
            OfflinePlayer owner = plugin.getServer().getOfflinePlayer(ownerUid);
            if (!owner.hasPlayedBefore()) {
                continue;
            }

            long lastPlayed = owner.getLastPlayed();
            long cur = System.currentTimeMillis();
            int noLoginTerm = (int) ((cur - lastPlayed) / (1000 * 60 * 60 * 24));
            if (noLoginTerm < threshold) {
                return true;
            }
        }

        return false;
    }

    public boolean purge(CommandSender executor, boolean ignoreCooldown) throws IllegalStateException {
        checkPlotFlag();

        long cooldown = getCooldown();
        if (!ignoreCooldown && cooldown > 0) {
            plugin.messages.sendMessage(executor, "gui.regen-cooldown",
                    Messages.mapOf("%cooldown%", String.valueOf(cooldown / 1000)));
            return false;
        }
        
        logPurge();
        region.getMembers().removeAll();
        region.getOwners().removeAll();
        UUID oldOwner = getPlotOwnerUid();
        setPlotOwnerUid(null);
        setKeepTerm(Long.MAX_VALUE);
        regen(executor, oldOwner);
        return true;
    }

    /**
     * TODO: clean code.
     */
    private void logPurge() {
        plugin.getLogger().info(() -> "Purging plot: " + region.getId());
        
        OfflinePlayer owner = null;
        try {
            UUID ownerUid = getPlotOwnerUid();
            if (ownerUid != null) {
                owner = plugin.getServer().getOfflinePlayer(ownerUid);
                String ownerName = owner.getName() != null ? owner.getName() : ownerUid.toString();
                plugin.getLogger().info("Plot owner: " + ownerName);
            }
        } catch (IllegalArgumentException ignored) {
        }

        StringBuilder sb = new StringBuilder();

        Set<UUID> protectionOwners = region.getOwners().getUniqueIds();
        if (!protectionOwners.isEmpty()) {
            for (UUID regionOwnerUid : region.getOwners().getUniqueIds()) {
                OfflinePlayer protectionOwner = plugin.getServer().getOfflinePlayer(regionOwnerUid);
                String ownerName = protectionOwner.getName() != null ? protectionOwner.getName() : protectionOwner.getUniqueId().toString();
                sb.append("  ").append(ownerName).append(" (last login = ")
                        .append(Instant.ofEpochMilli(protectionOwner.getLastPlayed())).append(")\n");
            }
            sb.deleteCharAt(sb.length() - 1);
            plugin.getLogger().info(() -> sb.toString());
        }

        try {
            Path logFolder = plugin.getDataFolder().toPath().resolve("plot-removal-log");
            if (!Files.exists(logFolder) || !Files.isDirectory(logFolder)) {
                Files.createDirectories(logFolder);
            }

            Path logFile = logFolder.resolve(region.getId() + (owner != null ? "(" + owner.getUniqueId() + ")" : "") + ".log");
            if (Files.exists(logFile)) {
                Files.delete(logFile);
            }
            Files.createFile(logFile);

            Files.write(logFile, sb.toString().getBytes());

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, e, () -> "Cannot make log file of plot deletion because of file I/O Error.");
        }
    }

    public void makeNonPlot() throws IllegalStateException {
        checkPlotFlag();
        region.setFlag(PlotFlag.get(), null);
    }

    public static Plot makePlot(PlotGUI plugin, ProtectedRegion region) {
        if (!isPlot(region)) {
            region.setFlag(PlotFlag.get(), "");
        }
        return load(plugin, region);
    }

    public static Plot load(PlotGUI plugin, ProtectedRegion region) throws IllegalStateException {
        return new Plot(plugin, region);
    }

    public static boolean isPlot(ProtectedRegion region) {
        return region.getFlag(PlotFlag.get()) != null;
    }

    public static Set<Plot> getPlots(PlotGUI plugin, World world) {
        return WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(world))
                .getRegions().values().stream()
                .filter(Plot::isPlot)
                .map(r -> load(plugin, r))
                .collect(Collectors.toSet());
    }
    
    public static Set<Plot> getPlots(PlotGUI plugin, World world, OfflinePlayer player) {
        Set<Plot> plots = getPlots(plugin, world);
        plots.removeIf(p -> !player.getUniqueId().equals(p.getPlotOwnerUid()));
        return plots;
    }
}
