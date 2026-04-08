package com.FriedOUDON.NickUdon.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.LinkedHashMap;
import java.util.Map;

final class NickUdonPermissions {
    private static final Map<String, Boolean> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("nickudon.use", true);
        DEFAULTS.put("nickudon.admin", false);
        DEFAULTS.put("nickudon.broadcast.*", false);
        DEFAULTS.put("nickudon.broadcast.alias", false);
        DEFAULTS.put("nickudon.broadcast.prefix", false);
        DEFAULTS.put("nickudon.broadcast.subtitle", false);
        DEFAULTS.put("nickudon.nickname", true);
        DEFAULTS.put("nickudon.nickname.others", false);
        DEFAULTS.put("nickudon.prefix", false);
        DEFAULTS.put("nickudon.prefix.others", false);
        DEFAULTS.put("nickudon.subtitle", true);
        DEFAULTS.put("nickudon.subtitle.others", false);
    }

    private NickUdonPermissions() {
    }

    static boolean isGrantedByDefault(String permission) {
        return DEFAULTS.getOrDefault(permission, false);
    }

    static boolean check(ServerCommandSource source, String permission) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        if (isGrantedByDefault(permission)) {
            return Permissions.check(source, permission, true);
        }
        return Permissions.check(source, permission, 2);
    }

    static boolean check(ServerPlayerEntity player, String permission) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        if (isGrantedByDefault(permission)) {
            return Permissions.check(player, permission, true);
        }
        return Permissions.check(player, permission, 2);
    }

    static void primeKnownNodes(MinecraftServer server) {
        ServerCommandSource source = server.getCommandSource();
        for (Map.Entry<String, Boolean> entry : DEFAULTS.entrySet()) {
            if (entry.getValue()) {
                Permissions.check(source, entry.getKey(), true);
            } else {
                Permissions.check(source, entry.getKey(), 2);
            }
        }
    }
}
