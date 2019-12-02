package net.okocraft.plotgui.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import net.okocraft.plotgui.PlotGUI;
import net.okocraft.plotgui.config.Plots;
import net.okocraft.plotgui.event.ProtectionRemoveEvent;
import net.okocraft.plotgui.event.ProtectionRenameEvent;

public class ProtectionListener implements Listener {

    private static final PlotGUI PLUGIN = PlotGUI.getInstance();
    private static final Plots PLOTS = Plots.getInstance();
    private static final ProtectionListener INSTANCE = new ProtectionListener();

    private ProtectionListener() {
    }

    public static ProtectionListener getInstance() {
        return INSTANCE;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, PLUGIN);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onProtectionRemoved(ProtectionRemoveEvent event) {
        if (isPlot(event.getRegion(), event.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProtectionRenamed(ProtectionRenameEvent event) {
        if (isPlot(event.getFromRegion(), event.getWorld())) {
            event.setCancelled(true);
        }
    }

    private boolean isPlot(ProtectedRegion plot, World world) {
        if (!PLOTS.getPlots().contains(plot.getId())) {
            return false;
        }
        
        Location signLocation = PLOTS.getSignLocation(plot.getId());
        if (signLocation == null || !BukkitAdapter.adapt(signLocation.getWorld()).equals(world)) {
            return false;
        }
        
        return true;
    }
}