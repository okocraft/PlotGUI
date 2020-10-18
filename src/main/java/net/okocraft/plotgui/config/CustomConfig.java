package net.okocraft.plotgui.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Class for manipulating yaml files.
 *
 * @author LazyGon
 */
abstract class CustomConfig {

    protected final Plugin plugin;
    private final File file;
    private final String name;
    private FileConfiguration config;

    CustomConfig(Plugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.file = new File(plugin.getDataFolder(), this.name);
        reload();
        if (file.isDirectory()) {
            throw new IllegalArgumentException("file must not be directory");
        }
    }

    CustomConfig(Plugin plugin, File file) {
        this.plugin = plugin;
        if (!file.isFile()) {
            throw new IllegalArgumentException("file must not be directory");
        }
        this.file = file;
        this.name = file.getName();
        reload();
    }

    /**
     * Gets FileConfiguration of {@code file}.
     *
     * @return FileConfiguration
     * @author LazyGon
     */
    protected FileConfiguration get() {
        if (config == null) {
            reload();
        }

        return config;
    }

    /**
     * Loads FileConfiguration from {@code file}.
     *
     * @author LazyGon
     */
    public void reload() {
        saveDefault();
        config = YamlConfiguration.loadConfiguration(file);
        Optional<InputStream> inputStream = Optional.ofNullable(plugin.getResource(name));
        inputStream.ifPresent(stream -> config.setDefaults(YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        )));
    }

    /**
     * Saves default file which is included in jar.
     *
     * @author LazyGon
     */
    public void saveDefault() {
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
    }

    /**
     * 設定ファイルを保存する。
     *
     * @author LazyGon
     */
    public void save() {
        if (config == null)
            return;
        try {
            get().save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + file, e);
        }
    }
}