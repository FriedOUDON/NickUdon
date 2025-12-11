# NickUdon

Paper 向けのニックネーム・プレフィックス・2 行目サブタイトル用プラグイン。チャット/表示名の整形、PlaceholderAPI、マルチ言語に対応。

## ハイライト
- ニックネーム/プレフィックスにカラーコード対応、デフォルト色と一意性チェック付き。
- サブタイトル（2 行目）にカラーコードと PlaceholderAPI 対応。
- 表示名/チャット/参加退出メッセージの書式を設定可能、スコアボード名札も更新。
- Bedrock プレイヤー向けの HEX ダウンサンプル設定。
- 言語ファイルは `plugins/NickUdon/lang/` に自動配置され、外部ファイルを優先読み込み。

## コマンド
- `/nickudon name|nick|alias|rename [player] <alias|clear>`
- `/nickudon prefix [player] <prefix|clear>`
- `/nickudon subtitle [player] <text|clear|on|off>`
- `/nickudon reload`
- `/nickudon lang <code>`
- `/name ...` (`/nickudon name ...` の短縮)

## 権限
- `nickudon.use` — 基本操作
- `nickudon.admin` — 管理/他人編集
- `nickudon.broadcast.*` (`alias`, `prefix`, `subtitle`) — 変更ブロードキャスト閲覧
- `nickudon.nickname`, `nickudon.nickname.others`
- `nickudon.prefix`, `nickudon.prefix.others`
- `nickudon.subtitle`, `nickudon.subtitle.others`
- `nickudon.payments.bypass.*`（`alias`, `prefix`, `subtitle` と各 `.others`）— 課金有効時の支払いをスキップ

## コンフィグ要点 (`plugins/NickUdon/config.yml`)
- `nameFormat`, `chatNameFormat`, `prefixFormat` とエイリアスなしの書式
- `commandAliases` — `/nickudon` の追加エイリアス
- `defaultAliasColor`, `defaultPrefixColor`, `subtitle.defaultColor`
- `subtitle.*` — 書式・オフセット・updateTicks・viewRange
- `chatOverride.enabled`, `displayOverride.onJoinQuit`
- `aliasUnique.*` — 一意性ルール
- `payments.*` — Vault 課金設定（上記権限でバイパス可能）
- `defaultLocale` — デフォルトロケール
- `bedrock.*` — HEX ダウンサンプルの切替

## PlaceholderAPI プレースホルダー
- `%nickudon_alias%`, `%nickudon_alias_stripped%`
- `%nickudon_prefix%`
- `%nickudon_display%`, `%nickudon_display_no_prefix%`
- `%nickudon_name%`

## 言語ファイル
- `lang/en_US.yml`, `lang/ja_JP.yml` を同梱。`plugins/NickUdon/lang/` に置いた外部ファイルを優先的に読み込み。

## ライセンス
- GPL-3.0-or-later
