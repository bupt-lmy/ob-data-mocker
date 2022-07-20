# 发布指南

版本发布参考过程， 共 3 个步骤

## 步骤 1 新建版本分支

基于 master 分支拉版本分支，如 `0.1.3_snapshot` 。

```shell
git checkout -b 0.1.3_snapshot
```

## 步骤 2 Coding then PR

修改版本号样例，注意 SNAPSHOT 需要为大写。

```shell
new_version="0.1.3-SNAPSHOT"
mvn versions:set -DnewVersion="${new_version}"
mvn versions:commit
```

## 步骤 3 Maven 仓库配置

发布到 Maven 仓库需要配置仓库鉴权帐密，配置在 `settings.xml` 中。

如可以维护在默认路径 `${HOME}/.m2/settings.xml`，增加以下配置

```shell
    <servers>
        <server>
            <id>maven-snapshot</id>
            <username>your_repo_server_username</username>
            <password>your_repo_server_password</password>
        </server>
    </servers>
```

配置属性说明

- `id` 值和 mock-parent 的 `pom.xml` 文件里 distributionManagement 中 repository 的 `id` 值需要匹配。
- `username`，`password` 信息请联系 山露、诣舟。

## 步骤 4 发布到 maven 仓库

```shell
#install to local
mvn clean install -Dmaven.test.skip=true

#deploy to maven repository server
mvn deploy -Dmaven.test.skip=true

#identify maven settings file
mvn deploy --settings=${HOME}/.m2/settings.xml -Dmaven.test.skip=true
```

## 步骤 5 发布后代码合并到 master 分支

自测完成后 提 PR 合并到 master 分支。
