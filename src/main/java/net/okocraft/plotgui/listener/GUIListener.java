package net.okocraft.plotgui.listener;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
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
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import net.okocraft.plotgui.PlotGUI;
import net.okocraft.plotgui.config.Config;
import net.okocraft.plotgui.config.Messages;
import net.okocraft.plotgui.config.Plots;
import net.okocraft.plotgui.gui.GUI;
import net.okocraft.plotgui.gui.PlayersGUI;

public class GUIListener implements Listener {
    
    private static final PlotGUI PLUGIN = PlotGUI.getInstance();
    private static final GUIListener INSTANCE = new GUIListener();

    private GUIListener() {
    }

    public static GUIListener getInstance() {
        return INSTANCE;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, PLUGIN);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    
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
            List<OfflinePlayer> onlinePlayers = Bukkit.getOnlinePlayers().stream().map(p -> (OfflinePlayer) p)
                    .collect(Collectors.toList());
            player.openInventory(new PlayersGUI(player, onlinePlayers, gui, 0).getInventory());
            break;
        case 2:
            player.closeInventory();
            List<OfflinePlayer> members = region.getMembers().getUniqueIds().stream().map(Bukkit::getOfflinePlayer)
                    .collect(Collectors.toList());
            player.openInventory(new PlayersGUI(player, members, gui, 2).getInventory());
            break;
        case 4:
            player.closeInventory();
            List<OfflinePlayer> ownerCanditates = region.getMembers().getUniqueIds().stream()
                    .map(Bukkit::getOfflinePlayer).collect(Collectors.toList());
            player.openInventory(new PlayersGUI(player, ownerCanditates, gui, 4).getInventory());
            break;
        case 6:
            player.closeInventory();
            List<OfflinePlayer> owners = region.getOwners().getUniqueIds().stream()
                    .map(Bukkit::getOfflinePlayer).collect(Collectors.toList());
            player.openInventory(new PlayersGUI(player, owners, gui, 6).getInventory());
            break;
        case 8:
            startRegenConversation(true, player, region);
            player.closeInventory();
            break;
        }
    }

    private void startRegenConversation(boolean abandon, Player player, ProtectedRegion region) {
        player.acceptConversationInput("n");
        Conversation conversation = createYesNoConversation(abandon ? "gui.confirm-abandon" : "gui.confirm-regen",
                player);
        conversation.addConversationAbandonedListener(abandandedEvent -> {
            if ((boolean) abandandedEvent.getContext().getSessionData("response")) {
                String plotName = region.getId();
                if (Plots.getInstance().regen(plotName, player) && abandon) {
                    Plots.getInstance().getOwners(plotName).forEach(owner -> Plots.getInstance().removeOwner(plotName, owner));
                    Plots.getInstance().getMembers(plotName).forEach(owner -> Plots.getInstance().removeMember(plotName, owner));
                    region.getMembers().clear();
                }
            }
        });
        conversation.begin();
    }

    private Conversation createYesNoConversation(String messageKey, Player player) {
        return new ConversationFactory(PLUGIN).withPrefix(
                arg -> ChatColor.translateAlternateColorCodes('&', Messages.getInstance().getMessage("plugin.prefix") + " "))
                .withLocalEcho(false)
                .withFirstPrompt(new ValidatingPrompt() {

                    @Override
                    public String getPromptText(ConversationContext context) {
                        return ChatColor.translateAlternateColorCodes('&',
                                Messages.getInstance().getMessage(messageKey));
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

        String uuid = head.getItemMeta().getPersistentDataContainer().get(Config.headUUIDKey,
                PersistentDataType.STRING);
        OfflinePlayer selectedPlayer;
        try {
            selectedPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            PLUGIN.getLogger().log(Level.WARNING, "Invalid uuid string is stored in skull item.", e);
            return;
        }

        // slot: action
        // 0: add-member
        // 2: remove-member
        // 4: add-owner
        // 6: remove-owner
        switch (gui.getPreviousGUIClickedSlot()) {
        case 0:
            Plots.getInstance().addMember(region.getId(), selectedPlayer);
            gui.getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
            break;
        case 2:
            Plots.getInstance().removeMember(region.getId(), selectedPlayer);
            gui.getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
            break;
        case 4:
            Plots.getInstance().addOwner(region.getId(), selectedPlayer);
            player.closeInventory();
            break;
        case 6:
            Plots.getInstance().removeOwner(region.getId(), selectedPlayer);
            player.closeInventory();
            break;
        }
    }
}