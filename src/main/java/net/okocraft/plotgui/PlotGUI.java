package net.okocraft.plotgui;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.plotgui.config.Config;
import net.okocraft.plotgui.config.Plots;
import net.okocraft.plotgui.listener.GUIListener;
import net.okocraft.plotgui.listener.SignListener;

public class PlotGUI extends JavaPlugin {

    private static PlotGUI instance;

    @Override
    public void onEnable() {
        Config.getInstance().reloadAllConfigs();
        SignListener.getInstance().start();
        GUIListener.getInstance().start();
        ProtectionListener.getInstance().start();

        Plots.getInstance().regenMultiRegions(
                Plots.getInstance().getInactivePlots(Config.getInstance().getPlotPurgeDays()),
                Bukkit.getConsoleSender());

        new ProtectionWatchTask().runTaskTimerAsynchronously(this, 0L, 20L);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Plugin) this);
        Bukkit.getScheduler().cancelTasks(this);
    }

    public static PlotGUI getInstance() {
        if (instance == null) {
            instance = (PlotGUI) Bukkit.getPluginManager().getPlugin("PlotGUI");
        }

        return instance;
    }
}
