package net.okocraft.plotgui.listener;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import net.okocraft.plotgui.Plot;
import net.okocraft.plotgui.PlotGUI;

@EqualsAndHashCode
@RequiredArgsConstructor
public class PlayerListener implements Listener {
    
    private final PlotGUI plugin;

    private static final Set<Player> preserve = new HashSet<>();

    public static void putApplication(Player player) {
        preserve.add(player);
    }

    public static boolean containsPreservationRequst(Player player) {
        return preserve.contains(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        for (World world : plugin.getServer().getWorlds()) {
            Plot.getPlots(plugin, world, event.getPlayer()).forEach(p -> {
                if (containsPreservationRequst(event.getPlayer())) {
                    p.setKeepTerm(System.currentTimeMillis() + plugin.config.getPreservePlotDays() * 24 * 60 * 60 * 1000);
                } else {
                    p.setKeepTerm(System.currentTimeMillis() + plugin.config.getPlotPurgeDays() * 24 * 60 * 60 * 1000);
                }
            });
        }

        preserve.remove(event.getPlayer());
    }
}
