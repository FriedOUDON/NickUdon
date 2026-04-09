# NickUdon

NickUdon は、Paper と Fabric の両方を対象にしたニックネーム・プレフィックス・肩書き表示プラグイン/Mod です。  
共通の整形・設定・保存・言語処理は `common` にまとめ、プラットフォーム固有の実装は `paper` と `fabric` に分けています。

## モジュール構成
- `common`: 共通フォーマッタ、プロフィール保存、設定アクセス抽象、言語メッセージ処理
- `paper`: Paper プラグインのエントリポイント、PlaceholderAPI 連携、Vault 課金、Bukkit/Paper イベント
- `fabric`: Fabric Mod のエントリポイント、Patbox Text Placeholder API 連携、Fabric コマンド/イベント、経済連携なし

## 主な機能
- ニックネーム、プレフィックス、2 行目のネームタグ表示
- レガシーカラー `&` と RGB HEX `#RRGGBB`
- 表示形式とチャット形式のカスタマイズ
- 正規化ルール付きのニックネーム重複チェック
- `en_US` / `ja_JP` の多言語メッセージ
- join/quit 表示上書きとブロードキャスト
- Paper/Fabric 共通の設定デフォルト

## プラットフォーム別メモ

### Paper
- Paper プラグインとして動作します
- 外部 placeholder には PlaceholderAPI を利用できます
- 経済連携には Vault を利用できます
- 設定ディレクトリ: `plugins/NickUdon/`

### Fabric
- 現在の対応バージョンは `Minecraft 1.21.11`
- Fabric Loader `0.18.4` 以上 `0.19.0` 未満をサポートします
- サーバーの `mods/` に `fabric-api` と Patbox `placeholder-api` が必要です
- 経済連携はありません。`payments.*` 設定と bypass 権限は Paper 専用です
- `fabric-permissions-api-v0` 経由で外部権限 Mod と連携できます
- LuckPerms を権限プロバイダとして利用できます。権限 Mod がない場合は Mod 内の既定値 (`true` または OP レベル) にフォールバックします
- 設定ディレクトリ: `config/nickudon/`

## コマンド
- `/nickudon name|nick|alias|rename [player] <alias|clear>`
- `/nickudon prefix [player] <prefix|clear>`
- `/nickudon subtitle [player] <text|clear|on|off>`
- `/nickudon reload`
- `/nickudon lang <code>`
- `/nickudon cleanupsubtitles` (Fabric 専用、管理者向け、旧バージョン由来の残留肩書き用防具立てを削除)
- `/name ...` (`/nickudon name ...` の短縮)

## 権限
- `nickudon.use`
- `nickudon.admin`
- `nickudon.broadcast.*` (`alias`, `prefix`, `subtitle`)
- `nickudon.nickname`, `nickudon.nickname.others`
- `nickudon.prefix`, `nickudon.prefix.others`
- `nickudon.subtitle`, `nickudon.subtitle.others`
- `nickudon.payments.bypass.*` (`alias`, `prefix`, `subtitle` とその `.others`)

Paper では `plugin.yml` で権限定義を公開しています。  
Fabric では `fabric-permissions-api-v0` を使って権限ノードを評価します。LuckPerms などの権限プロバイダがあればその設定を使い、未導入時は Mod の既定値にフォールバックします。

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
必要条件:
- Fabric Loader `0.18.4` 以上 `0.19.0` 未満

1. `:fabric:build` を実行
2. `fabric/build/libs/NickUdon-fabric-<version>.jar` を `mods/` に配置
3. あわせて次を `mods/` に配置
   - `fabric-api-0.141.3+1.21.11.jar`
   - `placeholder-api-2.8.2+1.21.10.jar` 以上
   - OP 以外にも細かく権限管理したい場合は LuckPerms などの Fabric 権限 Mod
4. `Minecraft 1.21.11` の Fabric サーバーで起動
5. 旧 Fabric 版の肩書き用防具立てが残留した場合は、その近くに立って `/nickudon cleanupsubtitles` を実行

## 設定ファイルと言語ファイル
- 共通のデフォルトリソースは `common/src/main/resources/` にあります
- Paper の実行時ファイルは `plugins/NickUdon/`
- Fabric の実行時ファイルは `config/nickudon/`
- 外部 `lang/<locale>.yml` は既存値を優先しつつ、不足キーだけ自動追加されます

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

使用方法は、導入している Patbox Text Placeholder API のバージョンに対応した構文を使ってください。

## ライセンス
- GPL-3.0-or-later
