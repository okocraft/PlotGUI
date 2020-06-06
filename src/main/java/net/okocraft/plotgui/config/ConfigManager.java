package net.okocraft.plotgui.config;

public class ConfigManager {
    
    private final Config config = new Config();
    private final Messages messages = new Messages();
    private final Plots plots = new Plots(config, messages);

    public Config getConfig() {
        return config;
    }

    public Messages getMessages() {
        return messages;
    }

    public Plots getPlots() {
        return plots;
    }

    public void reloadAllConfigs() {
        config.reload();
        messages.reload();
        plots.reload();
    }
}