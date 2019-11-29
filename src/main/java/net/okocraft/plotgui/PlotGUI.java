package net.okocraft.plotgui;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.plotgui.config.Config;
//import net.okocraft.plotgui.config.Plots;

// TODO: GUI開くときの音？

public class PlotGUI extends JavaPlugin {

    private static PlotGUI instance;

    @Override
    public void onEnable() {
        Config.getInstance().reloadAllConfigs();
        PlayerListener.getInstance().start();

        //Plots.getInstance().regenMultiRegions(
        //        Plots.getInstance().getInactiveClaims(Config.getInstance().getPlotPurgeDays()),
        //        Bukkit.getConsoleSender());
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
