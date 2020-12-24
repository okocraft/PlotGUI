package net.okocraft.plotgui.listener;

import java.util.ArrayList;
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

    private static final Set<String> COMMAND_FLAG = new HashSet<>(Arrays.asList(
        "flag",
        "f"
    ));

    private final PlotGUI plugin;

    @EventHandler
    public void onWorldGuardCommand(PlayerCommandPreprocessEvent event) {
        List<String> args = new ArrayList<>(Arrays.asList(event.getMessage().split(" ", -1)));
        if (!REGION_COMMANDS.contains(args.get(0).toLowerCase(Locale.ROOT))) {
            return;
        }

        String subCommand = args.get(1).toLowerCase(Locale.ROOT);
        if (COMMAND_REDEFINE.contains(subCommand)) {
            handleRedefineCommand(args, event);
        } else if (COMMAND_REMOVE.contains(subCommand)) {
            handleRemoveCommand(args, event);
        } else if (COMMAND_FLAG.contains(subCommand)) {
            handleFlagCommand(args, event);
        }
    }

    public void handleRedefineCommand(List<String> args, PlayerCommandPreprocessEvent event) {
        args.remove("-g");
        World world;
        int worldIndex = args.indexOf("-w");
        if (worldIndex != -1) {
            if (args.size() - 1 <= worldIndex) {
                return;
            }
            world = Bukkit.getWorld(args.get(worldIndex + 1));
            if (world == null) {
                return;
            }
            args.remove(worldIndex);
            args.remove(worldIndex + 1);
        } else {
            world = event.getPlayer().getWorld();
        }

        if (args.size() != 3) {
            return;
        }

        String id = args.get(2);
        ProtectedRegion region = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world)).getRegion(id);
        if (region != null && Plot.isPlot(region)) {
            plugin.messages.sendMessage(event.getPlayer(), "command.general.error.cannot-remove-or-redefine-plot");
            event.setCancelled(true);
        }
    }

    public void handleFlagCommand(List<String> args, PlayerCommandPreprocessEvent event) {
        args.remove("-e");
        args.remove("-eh");
        World world;
        int worldArg = args.indexOf("-w");
        if (worldArg != -1) {
            if (args.size() - 1 < worldArg + 1) {
                return;
            }
            world = Bukkit.getWorld(args.get(worldArg + 1));
            if (world == null) {
                return;
            }
            args.remove(worldArg);
            args.remove(worldArg + 1);
        } else {
            world = event.getPlayer().getWorld();
        }
        
        
        int groupArg = args.indexOf("-g");
        if (groupArg != -1) {
            if (args.size() - 1 < groupArg + 1) {
                return;
            }
            args.remove(groupArg);
            args.remove(groupArg + 1);
        }
        
        if ((args.size() == 4 || args.size() == 5) && args.get(3).equalsIgnoreCase("plotdata")) {
            plugin.messages.sendMessage(event.getPlayer(), "command.general.error.cannot-remove-or-redefine-plot");
            event.setCancelled(true);
        }
    }
    
    public void handleRemoveCommand(List<String> args, PlayerCommandPreprocessEvent event) {        
        args.remove("-f");
        args.remove("-u");
        World world;
        int worldIndex = args.indexOf("-w");
        if (worldIndex != -1) {
            if (args.size() - 1 <= worldIndex) {
                return;
            }
            world = Bukkit.getWorld(args.get(worldIndex + 1));
            if (world == null) {
                return;
            }
            args.remove(worldIndex);
            args.remove(worldIndex + 1);
        } else {
            world = event.getPlayer().getWorld();
        }
    
        if (args.size() != 3) {
            return;
        }
    
        String id = args.get(2);
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
                || PlayerStates.getState(player) == PlayerStates.State.WAITING_REGION_BOUNDARIES_EDIT
                || (PlayerStates.getState(player) == PlayerStates.State.WAITING_INPUT_FLAG_STRING_VALUE
                        && RegionEditing.getFlag(player).getName().equals("plotdata"))) {
            event.setCancelled(true);
            event.setMessage("q");
            plugin.messages.sendMessage(player, "command.general.error.cannot-remove-or-redefine-plot");
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
            plugin.messages.sendMessage(player, "command.general.error.cannot-remove-or-redefine-plot");
            RegionEditing.deleteRegion(player);
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
        String worldName = RegionEditing.getWorld(player);
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
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
