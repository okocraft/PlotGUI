package net.okocraft.plotgui.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import net.okocraft.plotgui.PlotGUI;

public class GUI implements InventoryHolder {

    private final Player owner;
    private final Inventory inventory;
    private final ProtectedRegion region;
    
    public GUI(PlotGUI plugin, Player player, ProtectedRegion region) {
        this.owner = player;
        this.region = region;
        this.inventory = Bukkit.createInventory(this, 9, plugin.messages.getMessage("gui.management-title"));


        ItemStack flame = plugin.config.getFlameIcon();
        inventory.setItem(0, plugin.config.getAddMemberIcon());
        inventory.setItem(1, flame);
        inventory.setItem(2, plugin.config.getRemoveMemberIcon());
        inventory.setItem(3, flame);
        inventory.setItem(4, plugin.config.getAddOwnerIcon());
        inventory.setItem(5, flame);
        inventory.setItem(6, plugin.config.getRemoveOwnerIcon());
        inventory.setItem(7, flame);
        inventory.setItem(8, plugin.config.getAbandonIcon());

        plugin.config.playGUIOpenSound(player);
    }

    /**
     * @return the owner
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * @return the region
     */
    public ProtectedRegion getRegion() {
        return region;
    }

    public static boolean isGUI(Inventory inventory) {
        return inventory != null && inventory.getHolder() != null && inventory.getHolder() instanceof GUI;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}