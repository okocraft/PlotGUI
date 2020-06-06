package net.okocraft.plotgui.gui;

import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import net.okocraft.plotgui.PlotGUI;
import net.okocraft.plotgui.config.Config;
import net.okocraft.plotgui.config.Messages;

public class PlayersGUI implements InventoryHolder {

    private final PlotGUI plugin = PlotGUI.getInstance();
    private final Config config = plugin.getConfigManager().getConfig();
    private final Messages messages = plugin.getConfigManager().getMessages();

    private final InventoryHolder previousGUI;
    private final int previousGUIClickedSlot;
    private final Inventory inventory = Bukkit.createInventory(this, 54,
            messages.getMessage("gui.playerlist-title"));
    private final Player owner;
    private final List<OfflinePlayer> players;
    private int page;

    public PlayersGUI(Player player, List<OfflinePlayer> players, InventoryHolder previous,
            int previousGUIClickedSlot) {
        this.owner = player;
        this.players = players;
        this.players.removeIf(Objects::isNull);
        this.previousGUI = previous;
        this.previousGUIClickedSlot = previousGUIClickedSlot;
        setPage(1);

        config.playGUIOpenSound(player);
    }

    public void setPage(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("The page cannot be less than 1");
        }
        if (getPage() == page) {
            return;
        }
        this.page = page;
        inventory.clear();

        inventory.setItem(49, config.getBackToMainIcon());

        if (page > 1) {
            inventory.setItem(45, config.getPreviousPageIcon(page - 1));
        }

        int maxPage = players.size() % 54 == 0 ? players.size() / 54 : players.size() / 54 + 1;
        if (page + 1 <= maxPage) {
            inventory.setItem(53, config.getNextPageIcon(page + 1));
        }

        int fromIndex = (page - 1) * 45;
        int toIndex = fromIndex + 44;
        toIndex = Math.min(toIndex, players.size());
        toIndex = Math.max(toIndex, 0);
        List<OfflinePlayer> subList = players.subList(fromIndex, toIndex);
        for (int i = 0; i < subList.size(); i++) {
            inventory.setItem(i, config.getPlayerHead(subList.get(i)));
        }

        

        new BukkitRunnable() {

            int loadingPage = page;
            int loadingSlot = 0;

            @Override
            public void run() {
                if (getPage() != loadingPage || loadingSlot >= 44) {
                    cancel();
                    return;
                }
                ItemStack item = getInventory().getItem(loadingSlot);
                if (item == null || item.getType() != Material.PLAYER_HEAD) {
                    loadingSlot++;
                    return;
                }

                if (subList.size() <= loadingSlot) {
                    cancel();
                    return;
                }

                setSkullOwner(item, subList.get(loadingSlot));
                loadingSlot++;
            }
        }.runTaskTimer(PlotGUI.getInstance(), 0L, 2L);
    }

    private void setSkullOwner(ItemStack head, OfflinePlayer player) {
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        head.setItemMeta(meta);
    }

    public int getPage() {
        return page;
    }

    /**
     * @return the owner
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * @return the previousGUI
     */
    public InventoryHolder getPreviousGUI() {
        return previousGUI;
    }

    /**
     * @return the clicked slot of previous gui
     */
    public int getPreviousGUIClickedSlot() {
        return previousGUIClickedSlot;
    }

    public static boolean isPlayersGUI(Inventory inventory) {
        return inventory != null && inventory.getHolder() != null && inventory.getHolder() instanceof PlayersGUI;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}