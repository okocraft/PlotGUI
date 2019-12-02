package net.okocraft.plotgui.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import net.okocraft.plotgui.PlotGUI;
// import net.okocraft.plotgui.config.Plots;
import net.okocraft.plotgui.event.ProtectionRemoveEvent;
import net.okocraft.plotgui.event.ProtectionRenameEvent;

public class ProtectionListener implements Listener {

    private static final PlotGUI PLUGIN = PlotGUI.getInstance();
    // private static final Plots PLOTS = Plots.getInstance();
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
        System.out.println(event.getRegion().getId());
    }

    @EventHandler
    public void onProtectionRenamed(ProtectionRenameEvent event) {
        System.out.println(event.getFromRegion().getId() + " -> " + event.getToRegion().getId());
        event.setCancelled(true);
    }
}