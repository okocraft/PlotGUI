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
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import net.okocraft.plotgui.PlotGUI;
import net.okocraft.plotgui.Utility;
import net.okocraft.plotgui.gui.GUI;

@EqualsAndHashCode
@AllArgsConstructor
public class SignListener implements Listener {

    private final PlotGUI plugin;

    private final Map<Player, String> confirm = new HashMap<>();

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
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

        String plotName = plugin.plots.getPlotBySignLocation(clicked.getLocation());
        if (plotName != null && Utility.getRegion(clicked.getWorld(), plotName) == null) {
            plugin.plots.removePlot(plotName);
            clicked.breakNaturally();
            plugin.messages.sendMessage(player, "other.no-plot-with-name", Map.of("%name%", plotName));
            return;
        }

        ProtectedRegion region = Utility.getRegionAtOrBihind(clicked);
        if (region == null) {
            return;
        }

        String regionId = region.getId();
        if (!plugin.plots.getPlots().contains(regionId)) {
            plugin.plots.addPlot(regionId, sign.getWorld(), sign.getLocation(),
                    Utility.getSignFace(sign.getBlock()).getOppositeFace(), null);
        }

        sign.setLine(1, regionId);

        Set<OfflinePlayer> owners = plugin.plots.getOwners(regionId);
        if (!owners.isEmpty()) {
            Optional<OfflinePlayer> owner = owners.stream().filter(element -> element.getName() != null).findAny();
            String ownerName = owner.map(OfflinePlayer::getName).orElse("null");
            sign.setLine(2, ownerName);
            if (owners.stream().map(OfflinePlayer::getUniqueId).anyMatch(player.getUniqueId()::equals)
                    || player.hasPermission("plotgui.mod")) {
                player.openInventory(new GUI(plugin, player, region).getInventory());
            } else {
                plugin.messages.sendMessage(player, "other.here-is-other-players-region",
                        Map.of("%owner%", ownerName));
            }

            sign.update();
            return;
        }

        sign.setLine(2, plugin.messages.getMessage("other.click-here-to-claim"));
        
        if (confirm.getOrDefault(player, "").equals(regionId)) {
            plugin.messages.sendMessage(player, "other.claim-success", Map.of("%region%", regionId));
            plugin.plots.addOwner(regionId, player);
            confirm.remove(player);
            sign.setLine(2, player.getName());

            sign.update();
            return;
        }

        if (!plugin.plots.hasPlot(player)) {
            plugin.messages.sendMessage(player, "other.confirm-claim");
            confirm.put(player, regionId);

            sign.update();
            return;
        }

        if (plugin.config.perWorldPlots() && plugin.plots.getPlots(player).stream()
                .noneMatch(playerPlot -> plugin.plots.getWorldName(playerPlot).equals(sign.getWorld().getName()))) {
            plugin.messages.sendMessage(player, "other.confirm-claim");
            confirm.put(player, regionId);
        } else {
            plugin.messages.sendMessage(player, "other.cannot-claim-anymore");
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
            plugin.messages.sendNoPermission(event.getPlayer(), "plotgui.sign.remove");
            event.setCancelled(true);
            return;
        }

        plugin.plots.removePlot(region.getId());
        plugin.messages.sendMessage(event.getPlayer(), "other.remove-plot");
    }

    @EventHandler
    public void onSignChanged(SignChangeEvent event) {
        if (!event.getLine(0).equalsIgnoreCase("[PlotGUI]")) {
            return;
        }

        if (!event.getPlayer().hasPermission("plotgui.sign.place")) {
            event.getBlock().breakNaturally();
            plugin.messages.sendNoPermission(event.getPlayer(), "plotgui.sign.place");
            return;
        }

        event.setLine(0, "[PlotGUI]");

        ProtectedRegion region = Utility.getRegionAtOrBihind(event.getBlock());
        if (region == null) {
            event.getBlock().breakNaturally();
            plugin.messages.sendMessage(event.getPlayer(), "other.no-protection");
            return;
        }

        OfflinePlayer owner = null;
        try {
            owner = Bukkit.getOfflinePlayer(region.getOwners().getUniqueIds().iterator().next());
        } catch (NoSuchElementException ignored) {
        }

        if (plugin.plots.getPlots().contains(region.getId())) {
            plugin.messages.sendMessage(event.getPlayer(), "other.sign-is-already-registered");
            event.getBlock().breakNaturally();
            plugin.plots.placeSign(region.getId());
            return;
        }

        if (Utility.isWallSign(event.getBlock())) {
            event.getBlock().getRelative(Utility.getSignFace(event.getBlock()).getOppositeFace())
                    .setType(Material.BEDROCK);
        } else {
            event.getBlock().getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
        }

        plugin.plots.addPlot(region.getId(), event.getBlock().getWorld(), event.getBlock().getLocation(),
                Utility.getSignFace(event.getBlock()), owner);

        event.setLine(1, region.getId());
        String line2 = (owner == null) ? plugin.messages.getMessage("other.click-here-to-claim")
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
            plugin.messages.sendNoPermission(event.getPlayer(), "plotgui.sign.place");
            return;
        }

        sign.setLine(0, "[PlotGUI]");

        ProtectedRegion region = Utility.getRegionAtOrBihind(block);
        if (region == null) {
            event.setCancelled(true);
            plugin.messages.sendMessage(event.getPlayer(), "other.no-protection");
            return;
        }

        OfflinePlayer owner = null;
        try {
            owner = Bukkit.getOfflinePlayer(region.getOwners().getUniqueIds().iterator().next());
        } catch (NoSuchElementException ignored) {
        }

        if (plugin.plots.getPlots().contains(region.getId())) {
            plugin.messages.sendMessage(event.getPlayer(), "other.sign-is-already-registered");
            event.setCancelled(true);
            plugin.plots.placeSign(region.getId());
            return;
        }

        if (Utility.isWallSign(block)) {
            block.getRelative(Utility.getSignFace(block).getOppositeFace()).setType(Material.BEDROCK);
        } else {
            block.getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
        }

        plugin.plots.addPlot(region.getId(), block.getWorld(), event.getBlock().getLocation(),
                Utility.getSignFace(event.getBlock()), owner);

        sign.setLine(1, region.getId());
        String line2 = (owner == null) ? plugin.messages.getMessage("other.click-here-to-claim")
                : Optional.ofNullable(owner.getName()).orElse("null");
        sign.setLine(2, line2);
        sign.setLine(3, "");

        sign.update();
    }
}