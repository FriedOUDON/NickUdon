package com.FriedOUDON.NickUdon.fabric;

import com.FriedOUDON.NickUdon.common.ConfigAccess;
import com.FriedOUDON.NickUdon.common.MutableConfigAccess;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

final class FabricYamlConfigAccess implements MutableConfigAccess {
    private final Path file;
    private final Consumer<String> logger;
    private final Yaml yaml;
    private Map<String, Object> root;

    private FabricYamlConfigAccess(Path file,
                                   Consumer<String> logger,
                                   Yaml yaml,
                                   Map<String, Object> root) {
        this.file = file;
        this.logger = logger;
        this.yaml = yaml;
        this.root = root;
    }

    static FabricYamlConfigAccess fromFile(Path file, Consumer<String> logger) {
        FabricYamlConfigAccess access = new FabricYamlConfigAccess(file, logger, createYaml(), new LinkedHashMap<>());
        access.load();
        return access;
    }

    static FabricYamlConfigAccess fromReader(Reader reader, Consumer<String> logger) {
        try {
            Object loaded = createYaml().load(reader);
            return new FabricYamlConfigAccess(null, logger, createYaml(), asMap(loaded));
        } catch (Exception e) {
            logger.accept("Failed to read YAML resource: " + e.getMessage());
            return new FabricYamlConfigAccess(null, logger, createYaml(), new LinkedHashMap<>());
        }
    }

    @Override
    public String getString(String path, String defaultValue) {
        Object value = getValue(path);
        return value instanceof String string ? string : defaultValue;
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = getValue(path);
        return value instanceof Boolean bool ? bool : defaultValue;
    }

    @Override
    public double getDouble(String path, double defaultValue) {
        Object value = getValue(path);
        return value instanceof Number number ? number.doubleValue() : defaultValue;
    }

    @Override
    public long getLong(String path, long defaultValue) {
        Object value = getValue(path);
        return value instanceof Number number ? number.longValue() : defaultValue;
    }

    @Override
    public List<String> getStringList(String path) {
        Object value = getValue(path);
        if (!(value instanceof List<?> list)) return List.of();

        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String string) out.add(string);
        }
        return out;
    }

    @Override
    public List<Integer> getIntegerList(String path) {
        Object value = getValue(path);
        if (!(value instanceof List<?> list)) return List.of();

        List<Integer> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Number number) out.add(number.intValue());
        }
        return out;
    }

    @Override
    public Set<String> getKeys(String path) {
        Object value = path == null || path.isBlank() ? root : getValue(path);
        if (!(value instanceof Map<?, ?> map)) return Set.of();

        Set<String> keys = new LinkedHashSet<>();
        for (Object key : map.keySet()) {
            if (key instanceof String string) keys.add(string);
        }
        return keys;
    }

    @Override
    public void set(String path, Object value) {
        if (file == null) return;

        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map<?, ?> map)) {
                next = new LinkedHashMap<String, Object>();
                current.put(parts[i], next);
            }
            current = castMap(next);
        }

        if (value == null) {
            current.remove(parts[parts.length - 1]);
        } else {
            current.put(parts[parts.length - 1], value);
        }
    }

    @Override
    public void save() {
        if (file == null) return;

        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                yaml.dump(root, writer);
            }
        } catch (IOException e) {
            logger.accept("Failed to save " + file.getFileName() + ": " + e.getMessage());
        }
    }

    @Override
    public void load() {
        if (file == null || !Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = asMap(yaml.load(reader));
        } catch (IOException e) {
            logger.accept("Failed to load " + file.getFileName() + ": " + e.getMessage());
        }
    }

    boolean mergeMissing(FabricYamlConfigAccess defaults) {
        if (defaults == null) {
            return false;
        }
        return mergeMissing(root, defaults.root);
    }

    private Object getValue(String path) {
        if (path == null || path.isBlank()) return root;

        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = map.get(part);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object loaded) {
        if (loaded instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    out.put(key, normalize(entry.getValue()));
                }
            }
            return out;
        }
        return new LinkedHashMap<>();
    }

    private static Object normalize(Object value) {
        if (value instanceof Map<?, ?> map) return asMap(map);
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            for (Object item : list) out.add(normalize(item));
            return out;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static boolean mergeMissing(Map<String, Object> target, Map<String, Object> defaults) {
        boolean changed = false;
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = normalize(entry.getValue());
            Object currentValue = target.get(key);

            if (currentValue == null) {
                target.put(key, defaultValue);
                changed = true;
                continue;
            }

            if (currentValue instanceof Map<?, ?> currentMap && defaultValue instanceof Map<?, ?> defaultMap) {
                changed |= mergeMissing((Map<String, Object>) currentMap, (Map<String, Object>) defaultMap);
            }
        }
        return changed;
    }

    private static Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        return new Yaml(options);
    }
}
