package net.okocraft.plotgui.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
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
import net.okocraft.plotgui.Plot;
import net.okocraft.plotgui.PlotGUI;
import net.okocraft.plotgui.SignUtil;
import net.okocraft.plotgui.config.Messages;
import net.okocraft.plotgui.gui.GUI;

@EqualsAndHashCode
@AllArgsConstructor
public class SignListener implements Listener {

    private static final Map<Player, String> CONFIRM = new HashMap<>();
    
    private final PlotGUI plugin;

    @EventHandler(ignoreCancelled = true)
    public void onSignClicked(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        Sign sign = SignUtil.getSignFrom(event.getClickedBlock());
        if (sign == null || !SignUtil.isPlotGUISign(sign)) {
            return;
        }

        ProtectedRegion region = SignUtil.getRegionAtOrBehind(sign);
        if (region == null) {
            sign.getBlock().breakNaturally();
            plugin.messages.sendMessage(player, "other.no-protection");
            return;
        }
        if (!Plot.isPlot(region)) {
            sign.getBlock().breakNaturally();
            plugin.messages.sendMessage(player, "protection-is-not-plot");
            return;
        }
        Plot plot = Plot.load(plugin, region);

        sign.setLine(1, region.getId());

        UUID plotOwnerId = plot.getPlotOwnerUid();
        if (plotOwnerId != null) {
            OfflinePlayer owner = plugin.getServer().getOfflinePlayer(plotOwnerId);
            String ownerName = owner.getName();
            if (ownerName == null) {
                plot.setPlotOwnerUid(null);
                sign.setLine(2, plugin.messages.getMessage("other.click-here-to-claim"));
                sign.update();
                return;
            }

            sign.setLine(2, ownerName);

            if (player.equals(owner) || player.hasPermission("plotgui.mod")) {
                player.openInventory(new GUI(plugin, player, region).getInventory());
            } else {
                plugin.messages.sendMessage(player, "other.here-is-other-players-region", Messages.mapOf("%owner%", ownerName));
            }
            sign.update();
        } else {
            sign.setLine(2, plugin.messages.getMessage("other.click-here-to-claim"));
        
            if (CONFIRM.getOrDefault(player, "").equals(region.getId())) {
                plugin.messages.sendMessage(player, "other.claim-success", Messages.mapOf("%region%", region.getId()));
                plot.setPlotOwnerUid(player.getUniqueId());
                plot.setKeepTerm(System.currentTimeMillis() + plugin.config.getPlotPurgeDays() + 24 * 60 * 60 * 1000);
                plot.getRegion().getOwners().addPlayer(player.getUniqueId());
                CONFIRM.remove(player);
                sign.setLine(2, player.getName());
                sign.update();
                return;
            }

            int plotCount;
            if (plugin.config.isPerWorldPlotLimit()) {
                plotCount = Plot.getPlots(plugin, player.getWorld(), player).size();
            } else {
                plotCount = (int) plugin.getServer().getWorlds().stream()
                        .flatMap(world -> Plot.getPlots(plugin, world, player).stream()).count();
            }

            if (plotCount < plugin.config.getPlotLimit()) {
                plugin.messages.sendMessage(player, "other.confirm-claim");
                CONFIRM.put(player, region.getId());
            } else {
                plugin.messages.sendMessage(player, "other.cannot-claim-anymore");
            }

            sign.update();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        
        // このプラグイン管理の看板の土台が壊されたかどうか。壊させない。
        Sign base = SignUtil.getSignFrom(event.getBlock().getRelative(BlockFace.UP));
        if (base != null && SignUtil.isPlotGUISign(base)) {
            event.setCancelled(true);
            return;
        }

        // このプラグイン管理の看板が壊されたかどうか
        // 壊されていたらファイルに保存してあるプロットを消す
        Sign sign = SignUtil.getSignFrom(event.getBlock());
        if (sign != null && SignUtil.isPlotGUISign(sign)) {
            ProtectedRegion region = SignUtil.getRegionAtOrBehind(sign);
            if (region == null || !Plot.isPlot(region)) {
                return;
            }
            
            if (event.getPlayer().hasPermission("plotgui.sign.remove")) {
                Plot.load(plugin, region).makeNonPlot();
                plugin.messages.sendMessage(event.getPlayer(), "other.remove-plot");
                return;
            }
            
            plugin.messages.sendNoPermission(event.getPlayer(), "plotgui.sign.remove");
            event.setCancelled(true);
            return;
        }
    }

    private boolean onPlotSignPlaced(Player whoPlaced, Block block, String[] signLines) {
        Sign sign = SignUtil.getSignFrom(block);
        if (sign == null) {
            return false;
        }
        if (!signLines[0].equalsIgnoreCase("[PlotGUI]")) {
            return false;
        }

        if (!whoPlaced.hasPermission("plotgui.sign.place")) {
            block.breakNaturally();
            plugin.messages.sendNoPermission(whoPlaced, "plotgui.sign.place");
            return false;
        }

        signLines[0] = "[PlotGUI]";

        ProtectedRegion region = SignUtil.getRegionAtOrBehind(sign);
        if (region == null) {
            block.breakNaturally();
            plugin.messages.sendMessage(whoPlaced, "other.no-protection");
            return false;
        }

        Plot plot = Plot.makePlot(plugin, region);

        UUID ownerUid = plot.getPlotOwnerUid();
        OfflinePlayer owner = ownerUid != null ? plugin.getServer().getOfflinePlayer(ownerUid) : null;

        // 看板の土台を岩盤にする。
        if (sign.getBlockData() instanceof WallSign) {
            block.getRelative(SignUtil.getFacing(sign).getOppositeFace())
                    .setType(Material.BEDROCK);
        } else {
            block.getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
        }

        signLines[1] = region.getId();
        signLines[2] = (owner == null) ? plugin.messages.getMessage("other.click-here-to-claim")
                : Optional.ofNullable(owner.getName()).orElse("null");
        signLines[3] = "";
        return true;
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onSignChanged(SignChangeEvent event) {
        onPlotSignPlaced(event.getPlayer(), event.getBlock(), event.getLines());
    
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignPlaced(BlockPlaceEvent event) {
        Sign sign = SignUtil.getSignFrom(event.getBlock());
        if (sign != null) {
            if (onPlotSignPlaced(event.getPlayer(), event.getBlock(), sign.getLines())) {
                sign.update();
            }
        }
    }
}