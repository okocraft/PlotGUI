package net.okocraft.plotgui.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import net.okocraft.plotgui.PlotGUI;

public final class Messages extends CustomConfig {
    
    public Messages(PlotGUI plugin) {
        super(plugin, "messages.yml");
    }

    public static Map<String, String> mapOf(String ... placeholders) {
        Map<String, String> result = new HashMap<>();
        Iterator<String> it = Arrays.asList(placeholders).iterator();
        String key;
        String value;
        while (true) {
            if (it.hasNext()) {
                key = it.next();
                if (it.hasNext()) {
                    value = it.next();
                    result.put(key, value);
                } else {
                    return result;
                }
            } else {
                return result;
            }
        }
    }

    /**
     * Send message to player.
     * 
     * @param player
     * @param addPrefix
     * @param path
     * @param placeholders
     */
    public void sendMessage(CommandSender sender, boolean addPrefix, String path, Map<String, String> placeholders) {
        String prefix = addPrefix ? get().getString("plugin.prefix", "&8[&6PlotGUI&8]&r") + " " : "";
        String message = prefix + getMessage(path);
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            message = message.replace(placeholder.getKey(), placeholder.getValue());
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        return;
    }

    /**
     * Send message to player.
     * 
     * @param player
     * @param path
     * @param placeholders
     */
    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        sendMessage(sender, true, path, placeholders);
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param path
     */
    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, Messages.mapOf());
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param addPrefix
     * @param path
     */
    public void sendMessage(CommandSender sender, boolean addPrefix, String path) {
        sendMessage(sender, addPrefix, path, Messages.mapOf());
    }

    /**
     * Gets message from key. Returned messages will not translated its color code.
     * 
     * @param path
     * @return
     */
    public String getMessage(String path) {
        return get().getString(path, path);
    }

    public void sendInvalidArgument(CommandSender sender, String invalid) {
        sendMessage(sender, "command.general.error.invalid-argument", Messages.mapOf("%argument%", invalid));
    }

    public void sendNoPermission(CommandSender sender, String permission) {
        sendMessage(sender, "command.general.error.no-permission", Messages.mapOf("%permission%", permission));
    }

    public void sendConsoleSenderCannotUse(CommandSender sender) {
        sendMessage(sender, "command.general.error.cannot-use-from-console");
    }

    public void sendPlayerCannotUse(CommandSender sender) {
        sendMessage(sender, "command.general.error.player-cannot-use");
    }

    public void sendNotEnoughArguments(CommandSender sender) {
        sendMessage(sender, "command.general.error.not-enough-arguments");
    }

    public void sendInvalidNumber(CommandSender sender, String number) {
        sendMessage(sender, "command.general.error.invalid-number", Messages.mapOf("%number%", number));
    }

    public void sendUsage(CommandSender sender, String usage) {
        sendMessage(sender, "command.general.info.usage", Messages.mapOf("%usage%", usage));
    }

    public void sendNoPlayerFound(CommandSender sender, String player) {
        sendMessage(sender, "command.general.error.no-player-found", Messages.mapOf("%player%", player));
    }
}