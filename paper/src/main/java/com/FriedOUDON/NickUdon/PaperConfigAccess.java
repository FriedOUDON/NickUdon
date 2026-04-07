package com.FriedOUDON.NickUdon;

import com.FriedOUDON.NickUdon.common.ConfigAccess;
import com.FriedOUDON.NickUdon.common.MutableConfigAccess;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

final class PaperConfigAccess implements MutableConfigAccess {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private final Supplier<ConfigurationSection> sectionSupplier;

    private PaperConfigAccess(JavaPlugin plugin,
                              File file,
                              YamlConfiguration yaml,
                              Supplier<ConfigurationSection> sectionSupplier) {
        this.plugin = plugin;
        this.file = file;
        this.yaml = yaml;
        this.sectionSupplier = sectionSupplier;
    }

    static PaperConfigAccess pluginConfig(JavaPlugin plugin) {
        return new PaperConfigAccess(plugin, null, null, plugin::getConfig);
    }

    static PaperConfigAccess fileConfig(JavaPlugin plugin, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return new PaperConfigAccess(plugin, file, yaml, () -> yaml);
    }

    static PaperConfigAccess readOnly(JavaPlugin plugin, YamlConfiguration yaml) {
        return new PaperConfigAccess(plugin, null, null, () -> yaml);
    }

    @Override
    public String getString(String path, String defaultValue) {
        return section().getString(path, defaultValue);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        return section().getBoolean(path, defaultValue);
    }

    @Override
    public double getDouble(String path, double defaultValue) {
        return section().getDouble(path, defaultValue);
    }

    @Override
    public long getLong(String path, long defaultValue) {
        return section().getLong(path, defaultValue);
    }

    @Override
    public List<String> getStringList(String path) {
        return section().getStringList(path);
    }

    @Override
    public List<Integer> getIntegerList(String path) {
        return section().getIntegerList(path);
    }

    @Override
    public Set<String> getKeys(String path) {
        ConfigurationSection root = section();
        if (path == null || path.isBlank()) return new LinkedHashSet<>(root.getKeys(false));

        ConfigurationSection child = root.getConfigurationSection(path);
        return child == null ? Set.of() : new LinkedHashSet<>(child.getKeys(false));
    }

    @Override
    public void set(String path, Object value) {
        if (yaml != null) yaml.set(path, value);
    }

    @Override
    public void save() {
        if (yaml == null || file == null) return;
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save " + file.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public void load() {
        if (yaml == null || file == null || !file.exists()) return;
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Failed to load " + file.getName() + ": " + e.getMessage());
        }
    }

    private ConfigurationSection section() {
        return sectionSupplier.get();
    }
}
