package com.FriedOUDON.NickUdon.common;

import java.util.List;
import java.util.Set;

public interface ConfigAccess {
    String getString(String path, String defaultValue);
    boolean getBoolean(String path, boolean defaultValue);
    double getDouble(String path, double defaultValue);
    long getLong(String path, long defaultValue);
    List<String> getStringList(String path);
    List<Integer> getIntegerList(String path);
    Set<String> getKeys(String path);

    default String getString(String path) {
        return getString(path, null);
    }
}
