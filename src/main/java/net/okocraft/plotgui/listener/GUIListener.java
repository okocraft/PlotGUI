package net.okocraft.plotgui.listener;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import net.okocraft.plotgui.Plot;
import net.okocraft.plotgui.PlotGUI;
import net.okocraft.plotgui.config.Messages;
import net.okocraft.plotgui.gui.GUI;
import net.okocraft.plotgui.gui.PlayersGUI;

@EqualsAndHashCode
@AllArgsConstructor
public class GUIListener implements Listener {
    
    private final PlotGUI plugin;
    
    @EventHandler
    public void onGUIClicked(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getClickedInventory();
        if (!GUI.isGUI(inv)) {
            return;
        }

        event.setCancelled(true);

        GUI gui = (GUI) inv.getHolder();
        if (!event.getWhoClicked().equals(gui.getOwner()) && !player.hasPermission("plotgui.mod")) {
            return;
        }

        if (gui.getInventory().getItem(event.getSlot()) == null) {
            return;
        }

        // slot: action
        // 0: add-member
        // 2: remove-member
        // 4: add-owner
        // 6: remove-owner
        // 8: abandon-and-regen
        ProtectedRegion region = gui.getRegion();
        switch (event.getSlot()) {
        case 0:
            player.closeInventory();
            List<OfflinePlayer> onlinePlayers = plugin.getServer().getOnlinePlayers().stream().map(p -> (OfflinePlayer) p)
                    .filter(canditates -> !region.getMembers().contains(canditates.getUniqueId()))
                    .filter(canditates -> !region.getOwners().contains(canditates.getUniqueId()))
                    .collect(Collectors.toList());
            player.openInventory(new PlayersGUI(plugin, player, onlinePlayers, gui, 0).getInventory());
            break;
        case 1:
            player.closeInventory();
            List<OfflinePlayer> members = region.getOwners().getUniqueIds().stream().map(plugin.getServer()::getOfflinePlayer).collect(Collectors.toList());
            player.openInventory(new PlayersGUI(plugin, player, members, gui, 2).getInventory());
            break;
        case 3:
            player.closeInventory();
            List<OfflinePlayer> ownerCanditates = plugin.getServer().getOnlinePlayers().stream().map(p -> (OfflinePlayer) p)
                    .filter(canditates -> !region.getOwners().contains(canditates.getUniqueId()))
                    .collect(Collectors.toList());
            player.openInventory(new PlayersGUI(plugin, player, ownerCanditates, gui, 4).getInventory());
            break;
        case 4:
            player.closeInventory();
            List<OfflinePlayer> owners = region.getOwners().getUniqueIds().stream().map(plugin.getServer()::getOfflinePlayer).collect(Collectors.toList());
            owners.removeIf(p -> p.getUniqueId().equals(player.getUniqueId()));
            player.openInventory(new PlayersGUI(plugin, player, owners, gui, 6).getInventory());
            break;
        case 6:
            PlayerListener.putApplication(player);
            player.closeInventory();
            break;
        case 8:
            startAbandonConversation(player, region);
            player.closeInventory();
            break;
        }
    }

    private void startAbandonConversation(Player player, ProtectedRegion region) {
        player.acceptConversationInput("n");
        Conversation conversation = createYesNoConversation("gui.confirm-abandon", player);
        conversation.addConversationAbandonedListener(abandandedEvent -> {
            if ((boolean) abandandedEvent.getContext().getSessionData("response")) {
                if (Plot.isPlot(region)) {
                    Plot plot = Plot.load(plugin, region);
                    if (!plot.purge(player, false)) {
                        plugin.messages.sendMessage(player, "gui.regen-cooldown", Messages.mapOf("%cooldown%", String.valueOf(plot.getCooldown() / 1000)));
                    }
                    
                } else {
                    plugin.messages.sendMessage(player, "other.protection-is-not-plot");
                }
            }
        });
        conversation.begin();
    }

    private Conversation createYesNoConversation(String messageKey, Player player) {
        return new ConversationFactory(plugin).withPrefix(
                arg -> ChatColor.translateAlternateColorCodes('&', plugin.messages.getMessage("plugin.prefix") + " "))
                .withLocalEcho(false)
                .withFirstPrompt(new ValidatingPrompt() {

                    @Override
                    public String getPromptText(ConversationContext context) {
                        return ChatColor.translateAlternateColorCodes('&',
                                plugin.messages.getMessage(messageKey));
                    }

                    @Override
                    protected boolean isInputValid(ConversationContext context, String input) {
                        return input.equals("y") || input.equals("n");
                    }

                    @Override
                    protected Prompt acceptValidatedInput(ConversationContext context, String input) {
                        if (!isInputValid(context, input)) {
                            return this;
                        }

                        context.setSessionData("response", input.equals("y"));
                        return END_OF_CONVERSATION;
                    }
                }).buildConversation(player);
    }

    @EventHandler
    public void onPlayersGUIClicked(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getClickedInventory();
        if (!PlayersGUI.isPlayersGUI(inv)) {
            return;
        }

        event.setCancelled(true);

        PlayersGUI gui = (PlayersGUI) inv.getHolder();
        if (!player.equals(gui.getOwner()) && !player.hasPermission("plotgui.mod")) {
            return;
        }

        if (gui.getInventory().getItem(event.getSlot()) == null) {
            return;
        }

        if (event.getSlot() == 45) {
            int prevPage = gui.getPage() - 1;
            if (prevPage > 0) {
                gui.setPage(prevPage);
            }
            return;
        } else if (event.getSlot() == 53) {
            gui.setPage(gui.getPage() + 1);
            return;
        } else if (event.getSlot() == 49) {
            player.closeInventory();
            player.openInventory(gui.getPreviousGUI().getInventory());
            return;
        }

        ProtectedRegion region = ((GUI) gui.getPreviousGUI()).getRegion();
        ItemStack head = gui.getInventory().getItem(event.getSlot());
        if (head.getType() != Material.PLAYER_HEAD) {
            return;
        }

        String uuid = head.getItemMeta().getPersistentDataContainer().get(plugin.config.headUUIDKey,
                PersistentDataType.STRING);
        OfflinePlayer selectedPlayer;
        try {
            selectedPlayer = plugin.getServer().getOfflinePlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Invalid uuid string is stored in skull item.", e);
            return;
        }

        // slot: action
        // 0: add-member
        // 2: remove-member
        // 4: add-owner
        // 6: remove-owner
        switch (gui.getPreviousGUIClickedSlot()) {
        case 0:
            region.getMembers().addPlayer(selectedPlayer.getUniqueId());
            gui.getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
            break;
        case 2:
            region.getMembers().removePlayer(selectedPlayer.getUniqueId());
            gui.getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
            break;
        case 4:
            region.getOwners().addPlayer(selectedPlayer.getUniqueId());
            player.closeInventory();
            break;
        case 6:
            region.getOwners().removePlayer(selectedPlayer.getUniqueId());
            player.closeInventory();
            break;
        }
    }
}