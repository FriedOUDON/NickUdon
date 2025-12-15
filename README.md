# NickUdon

Paper plugin for nicknames, prefixes, and a second nametag line with chat/display formatting, PlaceholderAPI hooks, and multi-lang messages.

## Highlights
- Nickname/prefix with color codes (legacy `&` and RGB hex `#RRGGBB`), default colors, and uniqueness checks.
- Subtitle (second nametag line) with color codes (legacy/hex) and PlaceholderAPI support.
- Configurable display/chat formats, join/quit overrides, and scoreboard nametag updates.
- Bedrock-friendly hex downsampling options.
- Lang files auto-copied/overridable under `plugins/NickUdon/lang/`.

## Commands
- `/nickudon name|nick|alias|rename [player] <alias|clear>`
- `/nickudon prefix [player] <prefix|clear>`
- `/nickudon subtitle [player] <text|clear|on|off>`
- `/nickudon reload`
- `/nickudon lang <code>`
- `/name ...` (shorthand for `/nickudon name ...`)

## Permissions
- `nickudon.use` — basic use
- `nickudon.admin` — admin + edit others
- `nickudon.broadcast.*` (`alias`, `prefix`, `subtitle`) — see change broadcasts
- `nickudon.nickname`, `nickudon.nickname.others`
- `nickudon.prefix`, `nickudon.prefix.others`
- `nickudon.subtitle`, `nickudon.subtitle.others`
- `nickudon.payments.bypass.*` (`alias`, `prefix`, `subtitle`, and their `.others` variants) — skip costs when payments are enabled

## Config highlights (`plugins/NickUdon/config.yml`)
- `nameFormat`, `chatNameFormat`, `prefixFormat`, alias-less variants
- `commandAliases` — extra aliases for `/nickudon`
- `defaultAliasColor`, `defaultPrefixColor`, `subtitle.defaultColor`
- `subtitle.*` — format, offsets, updateTicks, viewRange
- `chatOverride.enabled`, `displayOverride.onJoinQuit`
- `aliasUnique.*` — uniqueness rules
- `payments.*` — Vault costs per action (bypass via permissions above)
- `defaultLocale` — fallback locale
- `bedrock.*` — hex downsampling toggles

## Placeholders (PlaceholderAPI)
- `%nickudon_alias%`, `%nickudon_alias_stripped%`
- `%nickudon_prefix%`
- `%nickudon_chat%`
- `%nickudon_display%`, `%nickudon_display_no_prefix%`
- `%nickudon_name%`

## Lang files
- Bundled `lang/en_US.yml`, `lang/ja_JP.yml`; external copies in `plugins/NickUdon/lang/` override bundled ones.

## License
- GPL-3.0-or-later
