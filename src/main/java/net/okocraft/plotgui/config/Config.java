package net.okocraft.plotgui.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.okocraft.plotgui.PlotGUI;

public final class Config extends CustomConfig {

    public static final NamespacedKey headUUIDKey = new NamespacedKey(PlotGUI.getInstance(), "uuid");
    private static final Config INSTANCE = new Config();

    private Config() {
        super("config.yml");
    }

    public static Config getInstance() {
        return INSTANCE;
    }
    
    public void playGUIOpenSound(Player player) {
        Sound openSound;
        try {
            openSound = Sound.valueOf(get().getString("gui.open-sound", "BLOCK_CHEST_OPEN"));
        } catch (IllegalArgumentException e) {
            openSound = Sound.BLOCK_CHEST_OPEN;
            PlotGUI.getInstance().getLogger().log(Level.WARNING, "The sound specified in config is invalid.", e);
        }
        player.playSound(
                player.getLocation(),
                openSound,
                SoundCategory.MASTER,
                1,
                1
        );
    }

    public int getRegenCooldown() {
        return get().getInt("regen.cooldown", 600);
    }
    
    public int getPlotPurgeDays() {
        return get().getInt("regen.plot-purge-days", 60);
    }

    public int getRegenBlocksPerTickUnit() {
        return get().getInt("regen.blocks-per-unit", 16 * 16 * 32);
    }

    public long getRegenTickUnit() {
        return (long) get().getInt("regen.tick-unit", 10);
    }

    public ItemStack getAddMemberIcon() {
        return getIcon("add-member", Map.of());
    }

    public ItemStack getRemoveMemberIcon() {
        return getIcon("remove-member", Map.of());
    }

    public ItemStack getAddOwnerIcon() {
        return getIcon("add-owner", Map.of());
    }

    public ItemStack getRemoveOwnerIcon() {
        return getIcon("remove-owner", Map.of());
    }
    
    public ItemStack getAbandonIcon() {
        return getIcon("abandon-plot", Map.of());
    }

    public ItemStack getRegenIcon() {
        return getIcon("regen-plot", Map.of());
    }

    public ItemStack getFlameIcon() {
        return getIcon("flame", Map.of());
    }

    public ItemStack getPlayerHead(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        return getIcon("player-head", Map.of("%name%", Optional.ofNullable(player.getName()).orElse(uuid), "%uuid%", uuid));
    }

    public ItemStack getPreviousPageIcon(int previousPage) {
        return getIcon("previous-page", Map.of("%page%", String.valueOf(previousPage)));
    }

    public ItemStack getNextPageIcon(int nextPage) {
        return getIcon("next-page", Map.of("%page%", String.valueOf(nextPage)));
    }

    public ItemStack getBackToMainIcon() {
        return getIcon("back-to-main", Map.of());
    }

    private ItemStack getIcon(String iconKey, Map<String, String> placeholder) {
        if (!iconKey.startsWith("icons.")) {
            iconKey = "icons." + iconKey;
        }

        ItemStack icon;
        try {
            String material = iconKey.equals("icons.player-head")
                    ? "PLAYER_HEAD"
                    : get().getString(iconKey + ".material", "AIR");
            icon = new ItemStack(Material.valueOf(material));
            if (icon.getType() == Material.AIR) {
                return icon;
            }
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.AIR);
        }

        ItemMeta meta = icon.getItemMeta();
        String displayName = get().getString(iconKey + ".display-name");
        List<String> lore = get().getStringList(iconKey + ".lore");
        for (Map.Entry<String, String> pair : placeholder.entrySet()) {
            String key = pair.getKey();
            String value = pair.getValue();
            if (displayName != null) {
                displayName = displayName.replaceAll(key, value);
            }
            lore.replaceAll(line -> line.replaceAll(key, value));
        }

        if (displayName != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        }

        lore.replaceAll(line -> (line != null) ? ChatColor.translateAlternateColorCodes('&', line) : line);
        meta.setLore(lore);
        if (iconKey.equals("icons.player-head")) {
            meta.getPersistentDataContainer().set(headUUIDKey, PersistentDataType.STRING, placeholder.get("%uuid%"));
        }
        icon.setItemMeta(meta);
        return icon;
    }

    /**
     * Reload config. If this method used before {@code JailConfig.save()}, the data
     * on memory will be lost.
     */
    @Override
    public void reload() {
        Bukkit.getOnlinePlayers().forEach(Player::closeInventory);
        super.reload();
    }

    public void reloadAllConfigs() {
        reload();
        Messages.getInstance().reload();
        Plots.getInstance().reload();
    }
}