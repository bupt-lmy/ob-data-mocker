# 模拟数据工具

Mock数据即模拟数据工具用于批量产生测试数据，该工具研发的动机来源于两个方面：
* 第一是 ODC 2.4.0 的需求中包含模拟数据这一特性。
* 第二是为了丰富 OB 生态工具。

在日常的数据库开发过程中，往往存在需要向数据库中批量写入数据的需求，典型的场景例如 TPCC/H 测试时需要预先向数据库中写入一定数量的数据。目前，相关场景的模拟数据生成通常的解决方案是DBA编写存储过程批量地向数据库中写入或者自己编写简单的数据生成程序向数据库中写入。这样的方式的弊端主要是两点：
* DBA 或者用户编写的程序往往是针对特定场景的编程，程序的可复用性很低，可能会导致重复劳动。
* 手动编写的数据生成程序往往只能注入一些简单的无语义的数据，当面临语义数据注入的时候实现起来的成本就会很高。
模拟数据工具的实现就是为了给出一个通用的，功能全面的数据解决方案，让生成数据的成本降低，特别是能够生成有语义的数据。

## Overview

模拟数据工具分两种工作模式，分别为 Oracle 模式和 MySQL 模式，对应于 OB 的 Oracle 租户和 MySQL 租户。模拟数据工具针对每一种租户模式都进行了深度兼容，针对不同的数据类型给出了丰富的数据生成范式，允许用户根据自身的需要在工具能力范围内生成有语义的数据。目前版本的模拟数据工具兼容的Oracle数据类型包括：`NUMBER`,`CHAR`,`VARCHAR`,`VARCHAR2`,`NVARCHAR`,`DATE`,`TIMESTAMP`,`TIMESTAMP WITH TIME ZONE`,`TIMESTAMP WITH LOCAL TIME ZONE`,`INTERVAL YEAR TO MONTH`,`INTERVAL DAY TO SECOND`,`BLOB`,`CLOB`,`RAW`。MySQL模式下兼容的数据类型包括:`int`,`number`,`tinyint`,`smallint`,`mediumint`,`bigint`,`decimal`,`float`,`double`,`varchar`,`char`,`tinytext`,`mediumtext`,`text`,`longtext`,`tinyblob`,`blob`,`mediumblob`,`longblob`,`bit`,`binary`,`varbinary`,`timestamp`,`date`,`time`,`datetime`,`year`。

在生成数据的语义上，模拟数据工具允许用户配置多种类型的数据生成器，每种数据生成器均可根据配置生成带有语义的数据，典型的例如随机日期数据生成器，顺序日期数据生成器等等。

## 快速开始

以一个简单表的模拟数据写入为例子说明模拟数据工具的简单使用，目前模拟数据工具还只能以 SDK 的方式使用，未来可能会实现一个客户端更加友好地提供服务。
首先需要在 Java 工程中引入模拟数据工具的 pom 坐标：

```xml
<dependency>
  <groupId>com.oceanbase</groupId>
  <artifactId>ob-data-mocker</artifactId>
  <version>0.1.4</version>
</dependency>
```

随后在目标库中建立一个用于模拟数据插入的表：

```roomsql
CREATE TABLE "EMP" (
  "COL" NUMBER(5,2) NOT NULL,
  "COL2" VARCHAR(64) NOT NULL,
  "COL3" VARCHAR2(128) NOT NULL,
  "COL4" CHAR(128) NOT NULL,
  "COL5" NVARCHAR2(128) NOT NULL,
  "COL6" date,
  "COL7" interval year(5) to month,
  "COL8" interval day(2) to second(6),
  PRIMARY KEY ("COL", "COL4"),
  UNIQUE ("COL2", "COL3"),
  UNIQUE ("COL5")
);
```

然后再配置一个 json 文件用于配置的传入，这里的配置可以有多种传入途径，可以通过 json 文件，也可以通过 yaml 文件甚至可以通过代码上的 get/set 方法来传入配置：

```json
{
  "tables": [
    {
      "columns": [
        {
          "columnName": "COL",
          "typeConfig": {
            "name": "DIGIT",
            "columnType": "OB_ORACLE_NUMBER",
            "generator": "STEP_GENERATOR",
            "genParams": {
              "step": -1
            },
            "precision": 5,
            "scale": 2
          },
          "allowNull": false,
          "defaultValue": null,
          "dataType": null
        },
        {
          "columnName": "COL2",
          "typeConfig": {
            "name": "CHAR",
            "columnType": "OB_ORACLE_VARCHAR",
            "generator": "REGEXP_GENERATOR",
            "genParams": {
              "regText": "[a-z0-9A-Z]+\\@[a-z0-9A-Z]+\\.com"
            },
            "width": 64,
            "charset": "UTF_8"
          },
          "allowNull": false,
          "defaultValue": null,
          "dataType": null
        },
        {
          "columnName": "COL3",
          "typeConfig": {
            "name": "CHAR",
            "columnType": "OB_ORACLE_VARCHAR2",
            "genParams": {
              "start": "-100",
              "end": "100"
            },
            "generator": "RANDOM_NUMBER_GENERATOR",
            "width": 128,
            "charset": "UTF_8"
          },
          "allowNull": false,
          "defaultValue": null,
          "dataType": null
        },
        {
          "columnName": "COL4",
          "typeConfig": {
            "name": "CHAR",
            "lowValue": 64,
            "highValue": 128,
            "columnType": "OB_ORACLE_CHAR",
            "generator": "RANDOM_GENERATOR",
            "width": 128,
            "charset": "UTF_8"
          },
          "allowNull": false,
          "defaultValue": null,
          "dataType": null
        },
        {
          "columnName": "COL5",
          "typeConfig": {
            "name": "CHAR",
            "columnType": "OB_ORACLE_NVARCHAR",
            "generator": "RANDOM_GENERATOR",
            "width": 128,
            "charset": "UTF_8"
          },
          "allowNull": false,
          "defaultValue": null,
          "dataType": null
        },
        {
          "columnName": "COL6",
          "typeConfig": {
            "name": "DATE",
            "columnType": "OB_ORACLE_TIMESTAMP",
            "generator": "NULL_TIMESTAMP_GENERATOR"
          },
          "allowNull": true,
          "defaultValue": null,
          "dataType": null
        },
        {
          "columnName": "COL7",
          "typeConfig": {
            "name": "DATE",
            "columnType": "OB_ORACLE_INTERVAL_YEAR_TO_MONTH",
            "generator": "NULL_INTERVALYM_GENERATOR"
          },
          "allowNull": true,
          "defaultValue": null,
          "dataType": null
        },
        {
          "columnName": "COL8",
          "typeConfig": {
            "name": "DATE",
            "columnType": "OB_ORACLE_INTERVAL_DAY_TO_SECOND",
            "generator": "NULL_INTERVALYM_GENERATOR"
          },
          "allowNull": true,
          "defaultValue": null,
          "dataType": null
        }
      ],
      "totalCount": 4500,
      "strategy": "IGNORE",
      "batchSize": 240,
      "whetherTruncate": false,
      "tableName": "EMP",
      "schemaName": "SYS",
      "timeout": 180000,
      "location": "test/mock/emp.sql"
    }
  ],
  "dialectType": "OB_ORACLE",
  "dbConfig": {
    "host": "xxx",
    "port": 2881,
    "user": "xxx",
    "tenant": "xxx",
    "cluster": null,
    "password": "xxx",
    "defaultSchame": "SYS"
  },
  "llmConfig": {
    "enabled": true,
    "taskConfigPath": "emp_task.json",
    "endpoint": "https://api.deepseek.com/chat/completions",//注意：此处需要在系统环境变量中设置MODEL_API_KEY="your-api-key"
    "timeoutSeconds": 300
  }
  "taskName": null,
  "minConnectionSize": 4,
  "maxConnectionSize": 6,
  "connectionIncreasementStep": 2
}

```
如果llmConfig下enabled设置为true，则需要在相同目录下新建一个config.json文件用于向AI解释数据库配置，示例如下：
```json
{
  "taskName": "emp_demo",
  "output": {
    "dir": "test/obtest",          // 输出目录（必须可写）
    "filePrefix": "emp_seed",      // 输出文件名前缀
    "rows": 500,                   // 生成行数
    "batchSize": 100,              // 每条INSERT包含的 VALUES 数
    "schema": "obtest",            // 可选，用于反引号包裹时拼接
    "table": "emp"                 // 表名
  },
  "table": {
    "name": "emp",
    "uniqueConstraints": [["ID"]], // 可选：用于提醒唯一性（本实现不会查库，仅做去重保护）
    "columns": [
      {
        "name": "ID",
        "type": "INT",
        "nullable": false,
        "comment": "主键，自增（步进）",
        "generator": { "name": "STEP", "start": 1, "step": 1 }
      },
      {
        "name": "NAME",
        "type": "VARCHAR(50)",
        "nullable": false,
        "comment": "英文姓名（首字母大写）",
        "generator": { "name": "REGEXP", "pattern": "[A-Z][a-z]{3,8}" }
      },
      {
        "name": "AGE",
        "type": "INT",
        "nullable": true,
        "comment": "年龄 18~65 随机",
        "generator": { "name": "RANGE_INT", "start": 18, "end": 65 }
      },
      {
        "name": "HIRE_DATE",
        "type": "DATE",
        "nullable": true,
        "comment": "入职日期（2000-01-01 ~ 2025-01-01）随机",
        "generator": { "name": "RANDOM_DATE", "start": "2000-01-01", "end": "2025-01-01" }
      }
    ]
  }
}


```
实际的 Java 代码调用层面只需要不超过10行代码即可完成调用，需要说明的是模拟数据工具的 SDK 是异步的：

```java
import com.oceanbase.tools.datamocker.ObDataMocker;
import com.oceanbase.tools.datamocker.ObMockerFactory;
import com.oceanbase.tools.datamocker.schedule.MockContext;
public class App {
    public static void main(String[] args) {
        // 从json文件中反序列化出一个配置对象
        ObjectMapper mapper = new ObjectMapper();
        MockTaskConfig taskConfig = mapper.readValue(
                new File(App.class.getClassLoader().getResource("data-config.json").getFile()),
                MockTaskConfig.class);
        // 启动mock数据任务
        ObMockerFactory factory = new ObMockerFactory(taskConfig);
        ObDataMocker mocker = factory.create();
        // context中封装了模拟数据任务的上下文信息，也封装了模拟数据任务的句柄信息，可以对任务的中断等操作
        MockContext context = mocker.start();
        
    }
}
```