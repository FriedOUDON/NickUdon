package com.FriedOUDON.NickUdon.common;

public interface MutableConfigAccess extends ConfigAccess {
    void set(String path, Object value);
    void save();
    void load();
}
