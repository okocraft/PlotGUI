package net.okocraft.plotgui.gui;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import net.okocraft.plotgui.config.Config;
import net.okocraft.plotgui.config.Messages;

public class GUI implements InventoryHolder {

    private final Player owner;
    private final Inventory inventory = Bukkit.createInventory(this, 9, Messages.getInstance().getMessage("gui.management-title"));
    private final ProtectedRegion region;
    
    public GUI(Player player, ProtectedRegion region) {
        this.owner = player;
        this.region = region;

        Config config = Config.getInstance();
        ItemStack flame = config.getFlameIcon();
        inventory.setItem(0, config.getAddMemberIcon());
        inventory.setItem(1, flame);
        inventory.setItem(2, config.getRemoveMemberIcon());
        inventory.setItem(3, flame);
        inventory.setItem(4, config.getChangeOwnerIcon(region.getId()));
        inventory.setItem(5, flame);
        inventory.setItem(6, config.getRegenIcon());
        inventory.setItem(7, flame);
        inventory.setItem(8, config.getAbandonIcon());

        config.playGUIOpenSound(player);
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

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}