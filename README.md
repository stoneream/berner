# berner

![berner](./berner.png)

## About

http4s（もといcats/cats effect）の素振りでDiscordのBotを書いていた。  
が、ひどいことになったためJDAで書き直した。

## 機能

### hub

チャンネル **prefix**-suffix が存在するとき、 **hub**-**prefix** に投稿を集約する。  
(おそらく`hub-hub`チャンネルを作成すると無限ループを起こす。)  
集約されるメッセージは直下のスレッドも対象となる。  
また、メッセージが削除されたり編集された場合は転送先にも反映される。

### archiver

`/archive` コマンドでチャンネルのメッセージをテキストファイル（パスワード付きzipファイル）にする。  
ファイルのアップロード制限の考慮がされていないため履歴が多すぎるとうまく動作しない可能性がある。

## マイグレーションについて

### require

- [Command-line - Command-line tool - Flyway by Redgate • Database Migrations Made Easy.](https://flywaydb.org/documentation/usage/commandline/)

### 手順

```bash
$ flyway -configFiles="flyway.sample.conf" migrate

# WARNING: A Flyway License was not provided; fell back to Community Edition. Please contact sales at sales@flywaydb.org for license information.
# Flyway Community Edition 9.20.0 by Redgate
# See release notes here: https://rd.gt/416ObMi
# 
# Database: jdbc:mysql://127.0.0.1:13306/database_name (MariaDB 11.0)
# WARNING: Flyway upgrade recommended: MariaDB 11.0 is newer than this version of Flyway and support has not been tested. The latest supported version of MariaDB is 10.10.
# Schema history table `database_name`.`flyway_schema_history` does not exist yet
# Successfully validated 1 migration (execution time 00:00.013s)
# Creating Schema History table `database_name`.`flyway_schema_history` ...
# Current version of schema `database_name`: << Empty Schema >>
# Migrating schema `database_name` to version "1 - initial version"
# Successfully applied 1 migration to schema `database_name`, now at version v1 (execution time 00:00.039s)
```

## Discordの設定

### Botの権限周りの設定

![memo1](./memo1.png)

![memo2](./memo2.png)

不必要な権限もつけている。のでよしなに...

## Dockerイメージのビルド

```
sbt docker:publishLocal
```

### Ref

Docker Plugin — sbt-native-packager 1.9.0
documentation : https://www.scala-sbt.org/sbt-native-packager/formats/docker.html

## Dockerイメージの実行例

stoneream/berner-bot general | Docker Hub : https://hub.docker.com/repository/docker/stoneream/berner-bot/general

```
docker run -it --rm --env-file ./.env stoneream/berner-bot:VERSION
```

必要に応じて`--add-host=host.docker.internal:host-gateway`

## 環境変数

| 変数名                        | 説明               | 補足                    |
|----------------------------|------------------|-----------------------|
| BERNER_DISCORD_TOKEN       | DiscordのBotのトークン | Developer Portal から取得 |
| BERNER_DB_DEFAULT_HOST     | データベースのホスト       |                       |
| BERNER_DB_DEFAULT_PORT     | データベースのポート       |                       |
| BERNER_DB_DEFAULT_NAME     | データベースの名前        |                       |
| BERNER_DB_DEFAULT_USER     | データベースのユーザー名     |                       |
| BERNER_DB_DEFAULT_PASSWORD | データベースのパスワード     |                       |
| BERNER_ALERT_WEBHOOK_URL   | アラート通知用のWebhook  | logback.xmlを参照        |
