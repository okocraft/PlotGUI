package net.okocraft.plotgui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.plotgui.config.ConfigManager;
import net.okocraft.plotgui.config.Plots;
import net.okocraft.plotgui.event.ProtectionWatchTask;
import net.okocraft.plotgui.listener.GUIListener;
import net.okocraft.plotgui.listener.ProtectionListener;
import net.okocraft.plotgui.listener.SignListener;

// TODO: keep chunk on regen

public class PlotGUI extends JavaPlugin {

    private static PlotGUI instance;

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager();
        configManager.reloadAllConfigs();

        new SignListener().start();
        new GUIListener().start();
        new ProtectionListener().start();

        Plots plots = configManager.getPlots();
        plots.purgeInactivePlots(configManager.getConfig().getPlotPurgeDays());

        new ProtectionWatchTask().runTaskTimerAsynchronously(this, 0L, 20L);

        int count = 0;
        for (String plotName : plots.getPlots()) {
            ProtectedRegion region = plots.getRegion(plotName);
            if (region != null && region.getOwners().getUniqueIds().isEmpty()) {
                count++;
            } else if (region == null) {
                getLogger().warning(plotName + " is null.");
            }
        }
        getLogger().info("Plots without owners: " + count);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Plugin) this);
        Bukkit.getScheduler().cancelTasks(this);
    }

    public static PlotGUI getInstance() {
        if (instance == null) {
            instance = getPlugin(PlotGUI.class);
        }

        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
