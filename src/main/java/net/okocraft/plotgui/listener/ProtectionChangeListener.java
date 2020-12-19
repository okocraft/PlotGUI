package net.okocraft.plotgui.listener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mirotcz.wg_gui.PlayerStates;
import com.mirotcz.wg_gui.RegionEditing;
import com.mirotcz.wg_gui.WG_GUI;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.permissions.PermissionAttachment;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import net.okocraft.plotgui.Plot;
import net.okocraft.plotgui.PlotGUI;

@EqualsAndHashCode
@RequiredArgsConstructor
public class ProtectionChangeListener implements Listener {
    
    private static final Set<String> REGION_COMMANDS = new HashSet<>(Arrays.asList(
        "/worldguard:rg",
        "/worldguard:region",
        "/worldguard:regions",
        "/rg",
        "/region",
        "/regions"
    ));

    private static final Set<String> COMMAND_REMOVE = new HashSet<>(Arrays.asList(
        "remove",
        "rm",
        "delete",
        "del"
    ));

    private static final Set<String> COMMAND_REDEFINE = new HashSet<>(Arrays.asList(
        "redefine",
        "update",
        "move"
    ));

    private final PlotGUI plugin;

    @EventHandler
    public void onWorldGuardCommand(PlayerCommandPreprocessEvent event) {
        List<String> args = Arrays.asList(event.getMessage().split(" ", -1));
        String command = args.get(0).toLowerCase(Locale.ROOT);
        String subCommand = args.get(1).toLowerCase(Locale.ROOT);
        if (command.isEmpty() || !REGION_COMMANDS.contains(command)
                || (!COMMAND_REDEFINE.contains(subCommand) && !COMMAND_REMOVE.contains(subCommand))) {
            return;
        }

        World world;
        String id;
        int worldIndex = command.indexOf("-w") + 1;
        if (worldIndex != -1) {
            if (args.size() - 1 <= worldIndex + 1) {
                // 保護名がコマンドに含まれていない。
                return;
            }
            world = Bukkit.getWorld(args.get(worldIndex));
            if (world == null) {
                return;
            }
            id = args.get(worldIndex + 1);

        } else {
            world = event.getPlayer().getWorld();
            id = args.get(args.size() - 1);
        }

        ProtectedRegion region = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world)).getRegion(id);
        if (region != null && Plot.isPlot(region)) {
            plugin.messages.sendMessage(event.getPlayer(), "command.general.error.cannot-remove-or-redefine-plot");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWGGUIRegionModified(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ProtectedRegion region = getEditingRegion(player);
        if (region == null || !Plot.isPlot(region)) {
            return;
        }

        if (PlayerStates.getState(player) == PlayerStates.State.WAITING_INPUT_REGION_RENAME
                || PlayerStates.getState(player) == PlayerStates.State.WAITING_REGION_BOUNDARIES_EDIT) {
            event.setCancelled(true);
            event.setMessage("");
            return;
        }
    }

    private PermissionAttachment pa;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClickedLowest(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ProtectedRegion region = getEditingRegion(player);
        if (region == null || !Plot.isPlot(region)) {
            return;
        }
        if (WG_GUI.getPermsManager().get().checkPermission(player, "wggui.user.remove")) {
            pa = player.addAttachment(plugin);
            pa.setPermission("wggui.user.remove", false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClickedHighest(InventoryClickEvent event) {
        if (pa != null) {
            pa.getPermissible().removeAttachment(pa);
        }
    }

    private ProtectedRegion getEditingRegion(Player player) {
        if (player == null) {
            return null;
        }
        World world = Bukkit.getWorld(RegionEditing.getWorld(player));
        if (world == null) {
            return null;
        }
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        String regionName = RegionEditing.getRegion(player);
        if (regionName == null) {
            return null;
        }
        return rm.getRegion(regionName);
    }

}
