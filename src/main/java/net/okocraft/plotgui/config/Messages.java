package net.okocraft.plotgui.config;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Messages extends CustomConfig {
    
    Messages() {
        super("messages.yml");
    }

    /**
     * Send message to player.
     * 
     * @param player
     * @param addPrefix
     * @param path
     * @param placeholders
     */
    public void sendMessage(CommandSender sender, boolean addPrefix, String path, Map<String, Object> placeholders) {
        String prefix = addPrefix ? get().getString("plugin.prefix", "&8[&6PlotGUI&8]&r") + " " : "";
        String message = prefix + getMessage(path);
        for (Map.Entry<String, Object> placeholder : placeholders.entrySet()) {
            message = message.replace(placeholder.getKey(), placeholder.getValue().toString());
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
    public void sendMessage(CommandSender sender, String path, Map<String, Object> placeholders) {
        sendMessage(sender, true, path, placeholders);
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param path
     */
    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, Map.of());
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param addPrefix
     * @param path
     */
    public void sendMessage(CommandSender sender, boolean addPrefix, String path) {
        sendMessage(sender, addPrefix, path, Map.of());
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
        sendMessage(sender, "command.general.error.invalid-argument", Map.of("%argument%", invalid));
    }

    public void sendNoPermission(CommandSender sender, String permission) {
        sendMessage(sender, "command.general.error.no-permission", Map.of("%permission%", permission));
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
        sendMessage(sender, "command.general.error.invalid-number", Map.of("%number%", number));
    }

    public void sendUsage(CommandSender sender, String usage) {
        sendMessage(sender, "command.general.info.usage", Map.of("%usage%", usage));
    }

    public void sendNoPlayerFound(CommandSender sender, String player) {
        sendMessage(sender, "command.general.error.no-player-found", Map.of("%player%", player));
    }
}