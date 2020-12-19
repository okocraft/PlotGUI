package net.okocraft.plotgui;

import java.util.HashSet;
import java.util.Set;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.plotgui.config.Config;
import net.okocraft.plotgui.config.Messages;
import net.okocraft.plotgui.config.Plots;
import net.okocraft.plotgui.listener.GUIListener;
import net.okocraft.plotgui.listener.PlotPurgeListener;
import net.okocraft.plotgui.listener.ProtectionChangeListener;
import net.okocraft.plotgui.listener.SignListener;

/**
 * TODO: メッセージ送信メソッドを各メッセージに定義する。現状はパスをべた書きしている。
 */
public class PlotGUI extends JavaPlugin {

    public final Config config;
    public final Messages messages;

    public PlotGUI() {
        this.config = new Config(this);
        this.messages = new Messages(this);
    }

	@Override
	public void onLoad() {
        PlotFlag.register();
	}

    @Override
    public void onEnable() {
        reloadConfig();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new GUIListener(this), this);
        pm.registerEvents(new ProtectionChangeListener(this), this);
        pm.registerEvents(new SignListener(this), this);

        Set<Plot> inactivePlots = new HashSet<>();
        for (World world : getServer().getWorlds()) {
            Set<Plot> plots = Plot.getPlots(this, world);
            plots.removeIf(p -> !p.isInactive(config.getPlotPurgeDays(), false) || System.currentTimeMillis() < p.getKeepTerm());
            inactivePlots.addAll(plots);
        }

        if (!inactivePlots.isEmpty()) {
            getLogger().info("Purging " + inactivePlots.size() + " inactive plots.");
        }
        pm.registerEvents(new PlotPurgeListener(this, inactivePlots), this);

        migrate();
    }

    @Override
    public FileConfiguration getConfig() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("getConfig() is disabled. get config object with plugin.config;");
    }
    
    @Override
    public void reloadConfig() {
        config.reload();
        messages.reload();
    }

    private void migrate() {
        Plots plots = Plots.getInstance();

        for (String oldSystemPlotName : plots.getPlots()) {
            ProtectedRegion region = plots.getRegion(oldSystemPlotName);
            if (region == null) {
                System.out.println("there is not plot named " + oldSystemPlotName);
                continue;
            }
            int regenHeight = plots.getSignLocation(oldSystemPlotName).getBlockY() - 1;
            Set<OfflinePlayer> owners = plots.getOwners(oldSystemPlotName);
            OfflinePlayer mostActive = owners.isEmpty() ? null : owners.stream().max((p1, p2) -> (int) (p1.getLastPlayed() - p2.getLastPlayed())).get();
            
            Plot plot = Plot.makePlot(this, region);
            plot.setRegenHeight(regenHeight);
            if (mostActive != null) {
                plot.setPlotOwnerUid(mostActive.getUniqueId());
                plot.setKeepTerm(mostActive.getLastPlayed() + config.getPlotPurgeDays() * 24 * 60 * 60 * 1000);
            }

            
        }

    }
}
