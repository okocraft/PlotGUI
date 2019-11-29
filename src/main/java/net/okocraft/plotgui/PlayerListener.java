package net.okocraft.plotgui;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import net.md_5.bungee.api.ChatColor;
import net.okocraft.plotgui.config.Config;
import net.okocraft.plotgui.config.Messages;
import net.okocraft.plotgui.config.Plots;
import net.okocraft.plotgui.gui.GUI;
import net.okocraft.plotgui.gui.PlayersGUI;

public class PlayerListener implements Listener {

    private static final PlotGUI PLUGIN = PlotGUI.getInstance();
    private static final PlayerListener INSTANCE = new PlayerListener();

    private final Set<Player> confirm = new HashSet<>();

    private PlayerListener() {
    }

    static PlayerListener getInstance() {
        return INSTANCE;
    }

    void start() {
        Bukkit.getPluginManager().registerEvents(this, PLUGIN);
    }

    void stop() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onGUIClicked(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getClickedInventory();
        if (inv == null || inv.getHolder() == null || !(inv.getHolder() instanceof GUI)) {
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
        // 4: change-owner
        // 6: regen-plot
        // 8: abandon-and-regen
        ProtectedRegion region = gui.getRegion();
        switch (event.getSlot()) {
        case 0:
            // TODO: test
            player.closeInventory();
            List<OfflinePlayer> onlinePlayers = Bukkit.getOnlinePlayers().stream().map(p -> (OfflinePlayer) p)
                    .filter(p -> !region.getMembers().getUniqueIds().contains(p.getUniqueId()))
                    .collect(Collectors.toList());
            player.openInventory(new PlayersGUI(player, onlinePlayers, gui, 0).getInventory());
            break;
        case 2:
            player.closeInventory();
            List<OfflinePlayer> members = region.getMembers().getUniqueIds().stream().map(Bukkit::getOfflinePlayer)
                    .filter(member -> !Plots.getInstance().getOwner(region.getId()).equals(member))
                    .collect(Collectors.toList());
            player.openInventory(new PlayersGUI(player, members, gui, 2).getInventory());
            break;
        case 4:
            player.closeInventory();
            List<OfflinePlayer> ownerCanditates = region.getMembers().getUniqueIds().stream()
                    .map(Bukkit::getOfflinePlayer)
                    .filter(member -> !Plots.getInstance().getOwner(region.getId()).equals(member))
                    .collect(Collectors.toList());
            player.openInventory(new PlayersGUI(player, ownerCanditates, gui, 4).getInventory());
            break;
        case 6:
            startRegenConversation(false, player, region);
            break;
        case 8:
            startRegenConversation(true, player, region);
            break;
        }
    }

    private void startRegenConversation(boolean abandon, Player player, ProtectedRegion region) {
        player.acceptConversationInput("n");
        Conversation conversation = createYesNoConversation(abandon ? "gui.confirm-abandon" : "gui.confirm-regen",
                player);
        conversation.addConversationAbandonedListener(abandandedEvent -> {
            if ((boolean) abandandedEvent.getContext().getSessionData("response")) {
                Plots.getInstance().regen(region.getId(), player);
                if (abandon) {
                    Plots.getInstance().setOwner(region.getId(), null);
                }
            }
        });
        conversation.begin();
    }

    private Conversation createYesNoConversation(String messageKey, Player player) {
        return new ConversationFactory(PLUGIN).withPrefix(
                arg -> ChatColor.translateAlternateColorCodes('&', Messages.getInstance().getMessage("plugin.prefix")))
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
        if (inv == null || inv.getHolder() == null || !(inv.getHolder() instanceof PlayersGUI)) {
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
            selectedPlayer = Bukkit.getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            PLUGIN.getLogger().log(Level.WARNING, "Invalid uuid string is stored in skull item.", e);
            return;
        }

        if (selectedPlayer == null) {
            Messages.getInstance().sendNoPlayerFound(player, uuid);
            return;
        }

        // slot: action
        // 0: add-member
        // 2: remove-member
        // 4: change-owner
        switch (gui.getPreviousGUIClickedSlot()) {
        case 0:
            region.getMembers().addPlayer(WorldGuardPlugin.inst().wrapOfflinePlayer(selectedPlayer));
            Plots.getInstance().addMember(region.getId(), selectedPlayer);
            gui.getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
            break;
        case 2:
            region.getMembers().removePlayer(WorldGuardPlugin.inst().wrapOfflinePlayer(selectedPlayer));
            Plots.getInstance().removeMember(region.getId(), selectedPlayer);
            gui.getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
            break;
        case 4:
            Plots.getInstance().setOwner(region.getId(), selectedPlayer);
            player.closeInventory();
            break;
        }
    }

    @EventHandler
    public void onSignClicked(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null || !(clicked.getState() instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) clicked.getState();
        if (!sign.getLine(0).equals("[PlotGUI]")) {
            return;
        }

        ProtectedRegion region = getHereOrBackRegion(clicked);
        if (region == null) {
            return;
        }

        String regionId = region.getId();
        if (!Plots.getInstance().getClaims().contains(regionId)) {
            Plots.getInstance().addClaim(region.getId(), sign.getWorld(), sign.getLocation(),
                    Optional.ofNullable(getBackFace(sign.getBlock())).orElse(BlockFace.NORTH).getOppositeFace(), null);
        }

        sign.setLine(1, region.getId());

        Player player = event.getPlayer();

        OfflinePlayer owner = Plots.getInstance().getOwner(regionId);
        if (owner == null) {
            sign.setLine(2, Messages.getInstance().getMessage("other.click-here-to-claim"));
            if (Plots.getInstance().hasClaim(player)) {
                Messages.getInstance().sendMessage(player, "other.cannot-claim-anymore");

            } else if (confirm.contains(player)) {
                Messages.getInstance().sendMessage(player, "other.claim-success", Map.of("%region%", region.getId()));
                Plots.getInstance().setOwner(regionId, player);
                confirm.remove(player);
                region.getMembers().addPlayer(WorldGuardPlugin.inst().wrapPlayer(player));
                sign.setLine(2, player.getName());

            } else {
                Messages.getInstance().sendMessage(player, "other.confirm-claim");
                confirm.add(player);
            }
        } else {
            String ownerName = Optional.ofNullable(owner.getName()).orElse("null");
            sign.setLine(2, ownerName);
            if (player.getUniqueId().equals(owner.getUniqueId()) || player.hasPermission("plotgui.mod")) {
                player.openInventory(new GUI(player, region).getInventory());
            } else {
                Messages.getInstance().sendMessage(player, "other.here-is-other-players-region",
                        Map.of("%owner%", ownerName));
            }
        }

        sign.update();
    }

    @EventHandler
    public void onSignBroken(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (isSignOn(block)) {
            event.setCancelled(true);
            return;
        }

        if (!(block.getState() instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) block.getState();
        if (!sign.getLine(0).equals("[PlotGUI]")) {
            return;
        }

        ProtectedRegion region = getHereOrBackRegion(block);
        if (region == null) {
            return;
        }

        if (!event.getPlayer().hasPermission("plotgui.sign.remove")) {
            Messages.getInstance().sendNoPermission(event.getPlayer(), "plotgui.sign.remove");
            event.setCancelled(true);
            return;
        }

        Plots.getInstance().removeClaim(region.getId());
        Messages.getInstance().sendMessage(event.getPlayer(), "other.remove-plot");
    }

    private boolean isSignOn(Block block) {
        Block checking = block.getRelative(BlockFace.UP);
        if (checking.getBlockData() instanceof org.bukkit.block.data.type.Sign
                && ((Sign) checking.getState()).getLine(0).equals("[PlotGUI]")) {
            return true;
        }

        for (BlockFace face : Set.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH)) {
            checking = block.getRelative(face);
            if (checking.getBlockData() instanceof WallSign
                    && checking.getRelative(((WallSign) checking.getBlockData()).getFacing().getOppositeFace()).equals(block)
                    && ((Sign) checking.getState()).getLine(0).equals("[PlotGUI]")) {
                return true;
            }
        }

        return false;
    }

    @EventHandler
    public void onSignPlaced(SignChangeEvent event) {
        if (!event.getLine(0).equalsIgnoreCase("[PlotGUI]")) {
            return;
        }

        if (!event.getPlayer().hasPermission("plotgui.sign.place")) {
            event.getBlock().breakNaturally();
            Messages.getInstance().sendNoPermission(event.getPlayer(), "plotgui.sign.place");
            return;
        }

        event.setLine(0, "[PlotGUI]");

        ProtectedRegion region = getHereOrBackRegion(event.getBlock());
        if (region == null) {
            event.getBlock().breakNaturally();
            Messages.getInstance().sendMessage(event.getPlayer(), "other.no-protection");
            return;
        }

        OfflinePlayer owner = null;
        try {
            owner = Bukkit.getOfflinePlayer(region.getOwners().getUniqueIds().iterator().next());
        } catch (NoSuchElementException ignored) {
        }

        Plots plots = Plots.getInstance();

        if (plots.getClaims().contains(region.getId())) {
            Messages.getInstance().sendMessage(event.getPlayer(), "other.sign-is-already-registered");
            event.getBlock().breakNaturally();
            Plots.getInstance().placeSign(region.getId());
            return;
        }

        if (event.getBlock().getBlockData() instanceof WallSign) {
            event.getBlock().getRelative(((WallSign) event.getBlock().getBlockData()).getFacing().getOppositeFace()).setType(Material.BEDROCK);
        } else {
            event.getBlock().getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
        }

        plots.addClaim(region.getId(), event.getBlock().getWorld(), event.getBlock().getLocation(),
                getBackFace(event.getBlock()).getOppositeFace(), owner);

        event.setLine(1, region.getId());
        String line2 = (owner == null) ? Messages.getInstance().getMessage("other.click-here-to-claim")
                : Optional.ofNullable(owner.getName()).orElse("null");
        event.setLine(2, line2);
    }

    private ProtectedRegion getHereOrBackRegion(Block block) {
        ProtectedRegion region = null;
        region = getProtection(block);
        if (region == null) {
            BlockFace back = getBackFace(block);
            if (back == null) {
                return null;
            }

            region = getProtection(block.getRelative(back));
        }

        return region;
    }

    private ProtectedRegion getProtection(Block block) {
        Location location = block.getLocation();
        if (location.getWorld() == null) {
            return null;
        }
        BlockVector3 pos = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        ProtectedRegion region = new ProtectedCuboidRegion("__PLOTGUI_CHECKER__", pos, pos);

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null) {
            return null;
        }
        List<ProtectedRegion> intersecting = region.getIntersectingRegions(regionManager.getRegions().values());
        if (intersecting.size() != 1) {
            return null;
        }

        return intersecting.get(0);
    }

    private BlockFace getBackFace(Block block) {
        if (block.getBlockData() instanceof Rotatable) {
            return ((Rotatable) block.getBlockData()).getRotation().getOppositeFace();
        } else if (block.getBlockData() instanceof Directional) {
            return ((Directional) block.getBlockData()).getFacing().getOppositeFace();
        }
        return null;
    }
}