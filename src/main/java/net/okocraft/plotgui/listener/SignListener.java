package net.okocraft.plotgui.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import net.okocraft.plotgui.PlotGUI;
import net.okocraft.plotgui.Utility;
import net.okocraft.plotgui.config.Config;
import net.okocraft.plotgui.config.Messages;
import net.okocraft.plotgui.config.Plots;
import net.okocraft.plotgui.gui.GUI;

public class SignListener implements Listener {

    private static final PlotGUI PLUGIN = PlotGUI.getInstance();
    private static final Plots PLOTS = Plots.getInstance();
    private static final SignListener INSTANCE = new SignListener();

    private final Map<Player, String> confirm = new HashMap<>();

    private SignListener() {
    }

    public static SignListener getInstance() {
        return INSTANCE;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, PLUGIN);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onSignClicked(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null || !Utility.isSign(clicked)) {
            return;
        }

        Sign sign = (Sign) clicked.getState();

        if (!Utility.isPlotGUISign(clicked)) {
            return;
        }

        Player player = event.getPlayer();

        String plotName = PLOTS.getPlotBySignLocation(clicked.getLocation());
        if (plotName != null && Utility.getRegion(clicked.getWorld(), plotName) == null) {
            PLOTS.removePlot(plotName);
            clicked.breakNaturally();
            Messages.getInstance().sendMessage(player, "other.no-plot-with-name", Map.of("%name%", plotName));
            return;
        }

        ProtectedRegion region = Utility.getRegionAtOrBihind(clicked);
        if (region == null) {
            return;
        }

        String regionId = region.getId();
        if (!PLOTS.getPlots().contains(regionId)) {
            PLOTS.addPlot(regionId, sign.getWorld(), sign.getLocation(),
                    Utility.getSignFace(sign.getBlock()).getOppositeFace(), null);
        }

        sign.setLine(1, regionId);

        Set<OfflinePlayer> owners = PLOTS.getOwners(regionId);
        if (!owners.isEmpty()) {
            Optional<OfflinePlayer> owner = owners.stream().filter(element -> element.getName() != null).findAny();
            String ownerName = owner.map(OfflinePlayer::getName).orElse("null");
            sign.setLine(2, ownerName);
            if (owners.stream().map(OfflinePlayer::getUniqueId).anyMatch(player.getUniqueId()::equals)
                    || player.hasPermission("plotgui.mod")) {
                player.openInventory(new GUI(player, region).getInventory());
            } else {
                Messages.getInstance().sendMessage(player, "other.here-is-other-players-region",
                        Map.of("%owner%", ownerName));
            }

            sign.update();
            return;
        }

        sign.setLine(2, Messages.getInstance().getMessage("other.click-here-to-claim"));
        
        if (confirm.getOrDefault(player, "").equals(regionId)) {
            Messages.getInstance().sendMessage(player, "other.claim-success", Map.of("%region%", regionId));
            PLOTS.addOwner(regionId, player);
            confirm.remove(player);
            sign.setLine(2, player.getName());

            sign.update();
            return;
        }

        if (!PLOTS.hasPlot(player)) {
            Messages.getInstance().sendMessage(player, "other.confirm-claim");
            confirm.put(player, regionId);

            sign.update();
            return;
        }

        if (Config.getInstance().perWorldPlots() && PLOTS.getPlots(player).stream()
                .noneMatch(playerPlot -> PLOTS.getWorldName(playerPlot).equals(sign.getWorld().getName()))) {
            Messages.getInstance().sendMessage(player, "other.confirm-claim");
            confirm.put(player, regionId);
        } else {
            Messages.getInstance().sendMessage(player, "other.cannot-claim-anymore");
        }

        sign.update();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // このプラグイン管理の看板の土台が壊されたかどうか。壊させない。
        Block sign = Utility.getSignOn(block);
        if (sign != null && Utility.isPlotGUISign(sign)) {
            event.setCancelled(true);
            return;
        }

        // このプラグイン管理の看板が壊されたかどうか
        // 壊されていたらファイルに保存してあるプロットを消す
        if (!Utility.isSign(block) || !Utility.isPlotGUISign(block)) {
            return;
        }

        ProtectedRegion region = Utility.getRegionAtOrBihind(block);
        if (region == null) {
            return;
        }

        if (!event.getPlayer().hasPermission("plotgui.sign.remove")) {
            Messages.getInstance().sendNoPermission(event.getPlayer(), "plotgui.sign.remove");
            event.setCancelled(true);
            return;
        }

        PLOTS.removePlot(region.getId());
        Messages.getInstance().sendMessage(event.getPlayer(), "other.remove-plot");
    }

    @EventHandler
    public void onSignChanged(SignChangeEvent event) {
        if (!event.getLine(0).equalsIgnoreCase("[PlotGUI]")) {
            return;
        }

        if (!event.getPlayer().hasPermission("plotgui.sign.place")) {
            event.getBlock().breakNaturally();
            Messages.getInstance().sendNoPermission(event.getPlayer(), "plotgui.sign.place");
            return;
        }

        event.setLine(0, "[PlotGUI]");

        ProtectedRegion region = Utility.getRegionAtOrBihind(event.getBlock());
        if (region == null) {
            event.getBlock().breakNaturally();
            Messages.getInstance().sendMessage(event.getPlayer(), "other.no-protection");
            return;
        }

        OfflinePlayer owner = null;
        try {
            owner = Bukkit.getOfflinePlayer(region.getOwners().getUniqueIds().iterator().next());
        } catch (NoSuchElementException ignored) {
        }

        if (PLOTS.getPlots().contains(region.getId())) {
            Messages.getInstance().sendMessage(event.getPlayer(), "other.sign-is-already-registered");
            event.getBlock().breakNaturally();
            PLOTS.placeSign(region.getId());
            return;
        }

        if (Utility.isWallSign(event.getBlock())) {
            event.getBlock().getRelative(Utility.getSignFace(event.getBlock()).getOppositeFace())
                    .setType(Material.BEDROCK);
        } else {
            event.getBlock().getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
        }

        PLOTS.addPlot(region.getId(), event.getBlock().getWorld(), event.getBlock().getLocation(),
                Utility.getSignFace(event.getBlock()), owner);

        event.setLine(1, region.getId());
        String line2 = (owner == null) ? Messages.getInstance().getMessage("other.click-here-to-claim")
                : Optional.ofNullable(owner.getName()).orElse("null");
        event.setLine(2, line2);
        event.setLine(3, "");
    }

    @EventHandler
    public void onSignPlaced(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!Utility.isSign(block)) {
            return;
        }

        Sign sign = (Sign) block.getState();

        if (!sign.getLine(0).equalsIgnoreCase("[PlotGui]")) {
            return;
        }

        if (!event.getPlayer().hasPermission("plotgui.sign.place")) {
            event.setCancelled(true);
            Messages.getInstance().sendNoPermission(event.getPlayer(), "plotgui.sign.place");
            return;
        }

        sign.setLine(0, "[PlotGUI]");

        ProtectedRegion region = Utility.getRegionAtOrBihind(block);
        if (region == null) {
            event.setCancelled(true);
            Messages.getInstance().sendMessage(event.getPlayer(), "other.no-protection");
            return;

            // TODO: 設定が終わったらこのブロック内の以下のコードを消し去る。
            /*
            BlockFace oppositeFace = Utility.getSignFace(block).getOppositeFace();
            Block regionBlock = block.getRelative(BlockFace.DOWN).getRelative(oppositeFace);
            Block checking = regionBlock.getRelative(oppositeFace);
            while (checking.getType() == regionBlock.getType()) {
                checking = checking.getRelative(oppositeFace);
            }
            checking = checking.getRelative(oppositeFace.getOppositeFace());

            Block pos1 = null;
            Block pos2 = null;
            if (oppositeFace == BlockFace.NORTH || oppositeFace == BlockFace.SOUTH) {
                pos1 = regionBlock.getRelative(BlockFace.EAST);
                while (pos1.getType() == regionBlock.getType()) {
                    pos1 = pos1.getRelative(BlockFace.EAST);
                }
                pos1 = pos1.getRelative(BlockFace.WEST);

                pos2 = checking.getRelative(BlockFace.WEST);
                while (pos2.getType() == regionBlock.getType()) {
                    pos2 = pos2.getRelative(BlockFace.WEST);
                }
                pos2 = pos2.getRelative(BlockFace.EAST);
            } else if (oppositeFace == BlockFace.EAST || oppositeFace == BlockFace.WEST) {
                pos1 = regionBlock.getRelative(BlockFace.NORTH);
                while (pos1.getType() == regionBlock.getType()) {
                    pos1 = pos1.getRelative(BlockFace.NORTH);
                }
                pos1 = pos1.getRelative(BlockFace.SOUTH);

                pos2 = checking.getRelative(BlockFace.SOUTH);
                while (pos2.getType() == regionBlock.getType()) {
                    pos2 = pos2.getRelative(BlockFace.SOUTH);
                }
                pos2 = pos2.getRelative(BlockFace.NORTH);
            }

            if (pos1 == null || pos2 == null) {
                event.getPlayer().sendMessage("error on retrieving pos1 and pos2. aborted.");
                return;
            }

            int minX = Math.min(pos1.getLocation().getBlockX(), pos2.getLocation().getBlockX());
            int minZ = Math.min(pos1.getLocation().getBlockZ(), pos2.getLocation().getBlockZ());
            int maxX = Math.max(pos1.getLocation().getBlockX(), pos2.getLocation().getBlockX());
            int maxZ = Math.max(pos1.getLocation().getBlockZ(), pos2.getLocation().getBlockZ());
            BlockVector3 min = BlockVector3.at(minX, 0, minZ);
            BlockVector3 max = BlockVector3.at(maxX, 255, maxZ);
            region = new ProtectedCuboidRegion("plot_" + sign.getLocation().getBlockX() + "_" + sign.getLocation().getBlockZ(), min, max);            
            WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(sign.getWorld())).addRegion(region);
        } else if (!region.getId().startsWith("plot_")) {
            // NOTE: 保護領域改名イベントをキャンセルさせないようにしておく必要がある
            ProtectedRegion temp = new ProtectedCuboidRegion("plot_" + block.getLocation().getBlockX() + "_" + block.getLocation().getBlockZ(), region.getMinimumPoint(), region.getMaximumPoint());
            temp.copyFrom(region);
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(block.getWorld()));
            rm.addRegion(temp);
            for (ProtectedRegion child : rm.getRegions().values()) {
                if (child.getParent() != null && child.getParent().equals(region)) {
                    try {
                        child.setParent(temp);
                    } catch (CircularInheritanceException willNotCause) {
                    }
                }
            }
            rm.removeRegion(region.getId());
            region = temp;
             */
        }

        OfflinePlayer owner = null;
        try {
            owner = Bukkit.getOfflinePlayer(region.getOwners().getUniqueIds().iterator().next());
        } catch (NoSuchElementException ignored) {
        }

        if (PLOTS.getPlots().contains(region.getId())) {
            Messages.getInstance().sendMessage(event.getPlayer(), "other.sign-is-already-registered");
            event.setCancelled(true);
            PLOTS.placeSign(region.getId());
            return;
        }

        if (Utility.isWallSign(block)) {
            block.getRelative(Utility.getSignFace(block).getOppositeFace()).setType(Material.BEDROCK);
        } else {
            block.getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
        }

        PLOTS.addPlot(region.getId(), block.getWorld(), event.getBlock().getLocation(),
                Utility.getSignFace(event.getBlock()), owner);

        sign.setLine(1, region.getId());
        String line2 = (owner == null) ? Messages.getInstance().getMessage("other.click-here-to-claim")
                : Optional.ofNullable(owner.getName()).orElse("null");
        sign.setLine(2, line2);
        sign.setLine(3, "");

        sign.update();
    }
}