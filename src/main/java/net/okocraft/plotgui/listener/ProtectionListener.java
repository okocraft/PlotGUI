package net.okocraft.plotgui.listener;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import net.okocraft.plotgui.PlotGUI;
import net.okocraft.plotgui.config.Plots;
import net.okocraft.plotgui.event.ProtectionAddEvent;
import net.okocraft.plotgui.event.ProtectionRemoveEvent;
import net.okocraft.plotgui.event.ProtectionRenameEvent;

public class ProtectionListener implements Listener {

    private final PlotGUI plugin = PlotGUI.getInstance();
    private final Plots plots = plugin.getConfigManager().getPlots();

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    /**
     * 作成した保護がプロットに被っていたら作成をキャンセルするリスナー
     * 
     * @param event
     */
    @EventHandler
    public void onProtectionAdded(ProtectionAddEvent event) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(event.getWorld());
        Set<ProtectedRegion> plotsRegions = plots.getPlots().stream()
                .filter(plotName -> plots.getWorldName(plotName).equals(event.getWorld().getName()))
                .map(plotName -> rm.getRegion(plotName)).filter(Objects::nonNull).collect(Collectors.toSet());
        if (event.getRegion().getIntersectingRegions(plotsRegions).size() != 0) {
            event.setCancelled(true);
        }
    }

    /**
     * 消した保護がプロットだったら削除をキャンセルするリスナー。看板を破壊してプロット化を解除していれば削除できる。
     * 
     * @param event
     */
    @EventHandler
    public void onProtectionRemoved(ProtectionRemoveEvent event) {
        if (isPlot(event.getRegion(), event.getWorld())) {
            event.setCancelled(true);
        }
    }

    /**
     * どこかしらのプラグインがプロット保護の名前だけ変更したようなインスタンスを作成して大本の保護と入れ替えた場合、それをキャンセルするリスナー。看板を破壊してプロット化を解除していれば該当操作が可能になる。
     * 
     * @param event
     */
    @EventHandler
    public void onProtectionRenamed(ProtectionRenameEvent event) {
        if (isPlot(event.getFromRegion(), event.getWorld())) {
            event.setCancelled(true);
        }
    }

    private boolean isPlot(ProtectedRegion plot, World world) {
        if (!plots.getPlots().contains(plot.getId())) {
            return false;
        }

        Location signLocation = plots.getSignLocation(plot.getId());
        if (signLocation == null || !BukkitAdapter.adapt(signLocation.getWorld()).equals(world)) {
            return false;
        }

        return true;
    }
}