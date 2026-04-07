# NickUdon

NickUdon は、Paper と Fabric の両方を対象にしたニックネーム・プレフィックス・肩書き表示プラグイン/Mod です。
共通の整形処理、設定、保存、言語処理は `common` にまとめ、各プラットフォーム固有の実装を `paper` と `fabric` に分けています。

## モジュール構成
- `common`: フォーマッタ、プロフィール保存、設定アクセス抽象、言語メッセージ処理
- `paper`: Paper 用エントリポイント、PlaceholderAPI 連携、Vault 課金、Bukkit/Paper イベント
- `fabric`: Fabric 用エントリポイント、Patbox Text Placeholder API 連携、Fabric コマンド/イベント、経済連携なし

## 主な機能
- ニックネーム、プレフィックス、2 行目のネームタグ表示
- レガシーカラー `&` と RGB HEX `#RRGGBB`
- 表示名/チャット名フォーマットの設定
- 正規化付きのニックネーム重複チェック
- `en_US` / `ja_JP` の多言語メッセージ
- join/quit 表示上書きとブロードキャスト
- Paper/Fabric 共通の設定デフォルト

## プラットフォーム別の注意

### Paper
- Paper プラグインとして動作します
- 外部 placeholder 展開には PlaceholderAPI を利用します
- 経済課金には Vault を利用します
- 設定ディレクトリ: `plugins/NickUdon/`

### Fabric
- 現在の対象バージョンは `Minecraft 1.21.11` です
- Fabric Loader `0.18.4` 以上 `0.19.0` 未満をサポートします
- サーバーの `mods/` に `fabric-api` と Patbox `placeholder-api` が必要です
- 経済連携はありません。`payments.*` 系設定と bypass 権限は Paper 専用です
- `fabric-permissions-api-v0` 経由で外部権限 Mod と連携します
- Fabric では LuckPerms を権限プロバイダとして利用できます。権限 Mod がない場合は、Mod 内の既定値 (`true` または OP レベル) にフォールバックします
- 設定ディレクトリ: `config/nickudon/`

## コマンド
- `/nickudon name|nick|alias|rename [player] <alias|clear>`
- `/nickudon prefix [player] <prefix|clear>`
- `/nickudon subtitle [player] <text|clear|on|off>`
- `/nickudon reload`
- `/nickudon lang <code>`
- `/name ...` (`/nickudon name ...` の短縮)

## 権限
- `nickudon.use`
- `nickudon.admin`
- `nickudon.broadcast.*` (`alias`, `prefix`, `subtitle`)
- `nickudon.nickname`, `nickudon.nickname.others`
- `nickudon.prefix`, `nickudon.prefix.others`
- `nickudon.subtitle`, `nickudon.subtitle.others`
- `nickudon.payments.bypass.*` (`alias`, `prefix`, `subtitle` とその `.others`)

Paper では `plugin.yml` の権限定義を使います。
Fabric では `fabric-permissions-api-v0` を使って権限ノードを評価します。LuckPerms などの権限プロバイダを導入していればその設定が使われ、未導入時は Mod の既定値にフォールバックします。

## ビルド

### 前提
- Java `21`

### コマンド
- 両方ビルド: `.\gradlew build`
- Paper 版のみ: `.\gradlew :paper:build`
- Fabric 版のみ: `.\gradlew :fabric:build`

### 出力先
- Paper JAR: `paper/build/libs/NickUdon-<version>.jar`
- Fabric JAR: `fabric/build/libs/NickUdon-fabric-<version>.jar`

## 導入方法

### Paper
1. `:paper:build` を実行
2. `paper/build/libs/NickUdon-<version>.jar` を `plugins/` に配置
3. 外部 placeholder を使う場合は PlaceholderAPI を導入
4. `payments.*` を使う場合のみ Vault と経済プラグインを導入

### Fabric
必要な環境:
- Fabric Loader `0.18.4` 以上 `0.19.0` 未満

1. `:fabric:build` を実行
2. `fabric/build/libs/NickUdon-fabric-<version>.jar` を `mods/` に配置
3. あわせて次を `mods/` に配置
   - `fabric-api-0.141.3+1.21.11.jar`
   - `placeholder-api-2.8.2+1.21.10.jar` 以上
   - OP 以外にも細かく権限管理したい場合は LuckPerms などの Fabric 権限 Mod
4. `Minecraft 1.21.11` の Fabric サーバーで起動

## 設定ファイルと言語ファイル
- 共通のデフォルトリソースは `common/src/main/resources/` にあります
- Paper の実行時ファイルは `plugins/NickUdon/`
- Fabric の実行時ファイルは `config/nickudon/`
- `lang/<locale>.yml` を外部配置すると、同名の内蔵言語ファイルを上書きできます

## Placeholder

### Paper / PlaceholderAPI
- `%nickudon_alias%`, `%nickudon_alias_stripped%`
- `%nickudon_prefix%`
- `%nickudon_chat%`
- `%nickudon_display%`, `%nickudon_display_no_prefix%`
- `%nickudon_name%`

### Fabric / Patbox Text Placeholder API
NickUdon は次の placeholder identifier を登録します。
- `nickudon:alias`
- `nickudon:alias_stripped`
- `nickudon:prefix`
- `nickudon:chat`
- `nickudon:display`
- `nickudon:display_no_prefix`
- `nickudon:name`

実際の記法は、導入している Patbox Text Placeholder API のバージョンに従ってください。

## ライセンス
- GPL-3.0-or-later
