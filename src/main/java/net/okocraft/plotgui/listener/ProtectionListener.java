package net.okocraft.plotgui.listener;

import java.util.Set;
import java.util.stream.Collectors;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import net.okocraft.plotgui.Plot;
import net.okocraft.plotgui.PlotGUI;
import net.okocraft.plotgui.event.ProtectionAddEvent;
import net.okocraft.plotgui.event.ProtectionRemoveEvent;
import net.okocraft.plotgui.event.ProtectionRenameEvent;

@EqualsAndHashCode
@AllArgsConstructor
public class ProtectionListener implements Listener {

    private final PlotGUI plugin;

    /**
     * 作成した保護がプロットに被っていたら作成をキャンセルするリスナー
     * 
     * @param event
     */
    @EventHandler
    public void onProtectionAdded(ProtectionAddEvent event) {
        Set<ProtectedRegion> plotsRegions = Plot.getPlots(plugin, plugin.getServer().getWorld(event.getWorld().getName()))
                .stream().map(plot -> plot.getRegion()).collect(Collectors.toSet());
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
        if (Plot.isPlot(event.getRegion())) {
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
        if (Plot.isPlot(event.getFromRegion())) {
            event.setCancelled(true);
        }
    }
}