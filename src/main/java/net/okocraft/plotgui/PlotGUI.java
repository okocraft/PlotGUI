package net.okocraft.plotgui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.plotgui.config.Config;
import net.okocraft.plotgui.config.Messages;
import net.okocraft.plotgui.config.Plots;
import net.okocraft.plotgui.event.ProtectionWatchTask;
import net.okocraft.plotgui.listener.GUIListener;
import net.okocraft.plotgui.listener.ProtectionListener;
import net.okocraft.plotgui.listener.SignListener;

// TODO: keep chunk on regen

public class PlotGUI extends JavaPlugin {

    public final Config config;
    public final Messages messages;
    public final Plots plots;

    public PlotGUI() {
        this.config = new Config(this);
        this.messages = new Messages(this);
        this.plots = new Plots(this, config, messages);
    }

    @Override
    public void onEnable() {
        reloadConfig();
        plots.purgeInactivePlots(config.getPlotPurgeDays());

        new ProtectionWatchTask().runTaskTimerAsynchronously(this, 0L, 20L);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new GUIListener(this), this);
        pm.registerEvents(new ProtectionListener(this), this);
        pm.registerEvents(new SignListener(this), this);

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
    public FileConfiguration getConfig() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("getConfig() is disabled. get config object with plugin.config;");
    }
    
    @Override
    public void reloadConfig() {
        config.reload();
        messages.reload();
        plots.reload();
    }
}
