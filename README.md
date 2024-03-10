# berner

![berner](./berner.png)

## TODO

- 次の説明を記述する
  - 追加した環境変数
  - 追加した新機能
  - dockerイメージの起動方法が変わった件

## About

http4sの素振りでdiscordのbotを書いてる  
よくわからんまま書いたら闇鍋みたいになってる
実装がひどいことになってきたので直したい

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

## Dockerイメージのビルド

```
sbt docker:publishLocal
```

### Ref

Docker Plugin — sbt-native-packager 1.9.0 documentation : https://www.scala-sbt.org/sbt-native-packager/formats/docker.html

## Dockerイメージの実行例

stoneream/berner general - Docker Hub : https://hub.docker.com/repository/docker/stoneream/berner/general

```
docker run -it --rm --env-file ./.env berner:VERSION
```

必要に応じて`--add-host=host.docker.internal:host-gateway`

## 環境変数

| 変数名                     | 説明                     | 補足                      |
| -------------------------- | ------------------------ | ------------------------- |
| BERNER_DISCORD_TOKEN       | DiscordのBotのトークン   | Developer Portal から取得 |
| BERNER_DB_DEFAULT_HOST     | データベースのホスト     |                           |
| BERNER_DB_DEFAULT_PORT     | データベースのポート     |                           |
| BERNER_DB_DEFAULT_NAME     | データベースの名前       |                           |
| BERNER_DB_DEFAULT_USER     | データベースのユーザー名 |                           |
| BERNER_DB_DEFAULT_PASSWORD | データベースのパスワード |                           |

## メトリクスの取得

jmx-exporterを導入し、9090でlistenしている。  

### Ref

- prometheus/jmx_exporter: A process for exposing JMX Beans via HTTP for Prometheus consumption : https://github.com/prometheus/jmx_exporter
