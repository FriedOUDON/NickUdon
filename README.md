# NickUdon

NickUdon is a multi-target Minecraft nickname/prefix/subtitle project for Paper and Fabric.
Shared formatting, config, persistence, and localization logic lives in `common`, while platform-specific adapters live in `paper` and `fabric`.

## Modules
- `common`: shared formatter, profile repository, config access abstraction, and localized message handling
- `paper`: Paper plugin entrypoint, PlaceholderAPI integration, Vault-backed payments, Bukkit/Paper events
- `fabric`: Fabric mod entrypoint, Patbox Text Placeholder API integration, Fabric commands/events, no economy integration

## Highlights
- Nicknames, prefixes, and a second nametag line (subtitle)
- Legacy `&` colors and RGB hex `#RRGGBB`
- Configurable display/chat formats
- Alias uniqueness checks with configurable normalization rules
- Multi-language messages with bundled `en_US` and `ja_JP`
- Join/quit display overrides and broadcast messages
- Shared config defaults across Paper and Fabric

## Platform Notes

### Paper
- Built as a Paper plugin for the 1.21 API line
- Optional PlaceholderAPI integration for external placeholders
- Optional Vault integration for economy-backed payments
- Config directory: `plugins/NickUdon/`

### Fabric
- Current target: Minecraft `1.21.11`
- Supports Fabric Loader `0.18.4` through `0.18.x`
- Requires `fabric-api` and Patbox `placeholder-api` in the server `mods/` directory
- No economy integration; payment-related settings and bypass permissions are Paper-only
- Supports external permission providers through `fabric-permissions-api-v0`
- LuckPerms works as a permission provider on Fabric; without a provider, permission checks fall back to the built-in defaults (`true` or operator level)
- Config directory: `config/nickudon/`

## Commands
- `/nickudon name|nick|alias|rename [player] <alias|clear>`
- `/nickudon prefix [player] <prefix|clear>`
- `/nickudon subtitle [player] <text|clear|on|off>`
- `/nickudon reload`
- `/nickudon lang <code>`
- `/nickudon cleanupsubtitles` (Fabric only, admin only, removes orphan subtitle armor stands from older builds)
- `/name ...` (shorthand for `/nickudon name ...`)

## Permissions
- `nickudon.use`
- `nickudon.admin`
- `nickudon.broadcast.*` (`alias`, `prefix`, `subtitle`)
- `nickudon.nickname`, `nickudon.nickname.others`
- `nickudon.prefix`, `nickudon.prefix.others`
- `nickudon.subtitle`, `nickudon.subtitle.others`
- `nickudon.payments.bypass.*` (`alias`, `prefix`, `subtitle`, and their `.others` variants)

Paper exposes these permissions through `plugin.yml`.
Fabric uses `fabric-permissions-api-v0`. If a provider such as LuckPerms is installed, these permission nodes are checked there. Otherwise, Fabric falls back to the built-in defaults used by the mod.

## Build

### Requirements
- Java `21`

### Commands
- Build both targets: `.\gradlew build`
- Build Paper only: `.\gradlew :paper:build`
- Build Fabric only: `.\gradlew :fabric:build`

### Output
- Paper JAR: `paper/build/libs/NickUdon-<version>.jar`
- Fabric JAR: `fabric/build/libs/NickUdon-fabric-<version>.jar`

## Installation

### Paper
1. Build `:paper:build`
2. Put `paper/build/libs/NickUdon-<version>.jar` into `plugins/`
3. Install PlaceholderAPI if you want external placeholder expansion
4. Install Vault plus an economy plugin only if you want `payments.*` to be active

### Fabric
Requirements:
- Fabric Loader `0.18.4` through `0.18.x`

1. Build `:fabric:build`
2. Put `fabric/build/libs/NickUdon-fabric-<version>.jar` into `mods/`
3. Also install:
   - `fabric-api-0.141.3+1.21.11.jar`
   - `placeholder-api-2.8.2+1.21.10.jar` or newer
   - LuckPerms or another Fabric permission provider if you want non-OP permission management
4. Start a Minecraft `1.21.11` Fabric server
5. If older Fabric subtitle armor stands are left behind, stand near them and run `/nickudon cleanupsubtitles`

## Config And Lang Files
- Shared default resources are packaged from `common/src/main/resources/`
- Paper runtime files live under `plugins/NickUdon/`
- Fabric runtime files live under `config/nickudon/`
- External `lang/<locale>.yml` files override bundled resources on both platforms

## Placeholders

### Paper PlaceholderAPI
- `%nickudon_alias%`, `%nickudon_alias_stripped%`
- `%nickudon_prefix%`
- `%nickudon_chat%`
- `%nickudon_display%`, `%nickudon_display_no_prefix%`
- `%nickudon_name%`

### Fabric Patbox Text Placeholder API
NickUdon registers these placeholder identifiers:
- `nickudon:alias`
- `nickudon:alias_stripped`
- `nickudon:prefix`
- `nickudon:chat`
- `nickudon:display`
- `nickudon:display_no_prefix`
- `nickudon:name`

Use them with the syntax supported by the installed Patbox Text Placeholder API version.

## License
- GPL-3.0-or-later
