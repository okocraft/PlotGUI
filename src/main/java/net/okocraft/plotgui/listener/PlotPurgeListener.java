package net.okocraft.plotgui.listener;

import java.util.Map;
import java.util.Set;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import lombok.EqualsAndHashCode;
import net.okocraft.plotgui.Plot;
import net.okocraft.plotgui.PlotGUI;
import net.okocraft.plotgui.event.PlotRegenCompleteEvent;

@EqualsAndHashCode
public class PlotPurgeListener implements Listener {
    
    private final PlotGUI plugin;

    private final Set<Plot> scheduledPlots;

    private long initialPurgeElapsedTime = 0;

    private boolean inPluginInitializeState = true;

    public PlotPurgeListener(PlotGUI plugin, Set<Plot> scheduledPlots) {
        this.plugin = plugin;
        this.scheduledPlots = scheduledPlots;
        if (scheduledPlots.isEmpty()) {
            inPluginInitializeState = false;
        }
    }

    @EventHandler
    public void onPurgeComplete(PlotRegenCompleteEvent event) {
        if (inPluginInitializeState) {
            initialPurgeElapsedTime += event.getElapsedTime();
            
            scheduledPlots.remove(event.getRegeneratedPlot());
            if (scheduledPlots.isEmpty()) {
                plugin.getLogger().info("Purging inactive plots Completed: (" + initialPurgeElapsedTime + "ms)");
                inPluginInitializeState = false;
                return;
            }
            Plot plot = scheduledPlots.iterator().next();
            String oldOwnerName = event.getPreviousOwner() == null ? null : plugin.getServer().getOfflinePlayer(event.getPreviousOwner()).getName();
            plugin.getLogger().info("Purging " + plot.getRegion().getId() + " (owned by " + oldOwnerName + ")");
            plot.purge(event.getExecutor(), true);
        } else {
            plugin.messages.sendMessage(event.getExecutor(), "gui.regen-finish",
                    Map.of("%time%", String.valueOf(((double) event.getElapsedTime()) / 1000D))
            );
        }
    }
}
