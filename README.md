{  
"dpHttpUrl": "http://192.168.10.11:12345",  
"dpToken": "139c4d43544b5b3f946cf1632de8eb0f",  
"dpTenant": "yanjin",  
"dpProjectCode":"",  
"dpProjectName":"数据同步3",  
"inputJdbcUrl": "jdbc:postgresql://192.168.10.12:5432/pgtest?currentSchema=my_schema", // 对于postgresql，若schema非public，应指定  
"inputUserName":"postgres",  
"inputPassword":"postgres",  
"outputJdbcUrl": "jdbc:mysql://192.168.10.11:3306/test?characterEncoding=UTF-8",  
"outputUserName":"root",  
"outputPassword":"root",  
"errorLimit":0, // 脏数据条数最大值，主键或唯一键重复即脏数据   
"specified":"99 as id,'张三' as name", // 直接指定值字段，用逗号分隔，形式为xx as xx,xx as xx  
"where":"sex='男'", // 来源表过滤条件，不填则全部同步  
"deleteWhere":"1=1", // 同步前去向表删除条件：值为"truncate"或"1=1"，则清空表；为空字符串""或"1=2"则不删除任何数据；为其他内容则作为删除过滤条件  
"cycle":"minute:15", // 调度周期，不填默认为每天，"minute:15"代表每15分钟执行一次，"hour:2"代表每2小时执行一次  
"hourBegin":0, // 任务队列起始小时  
"minuteBegin":10, // 任务队列起始分钟  
"tableDispatchBatchSize":2, // 同一批次含有的任务数量  
"tableDispatchBatchInterval":3, // 批次间隔（分钟）  
"executeImmediately":"yes", // 值为"yes"或"true"则立即执行一次，否则不执行  
"prefix":"o_full_", // 去向表的前缀  
"suffix":"o_full_", // 去向表的后缀  
"tables":"test1,ff,table1,sys_user",  
"hiveStorageFormat":"", // 仅 CreateHiveTable 使用：Hive 存储格式(STORED AS)，留空默认 orc，也可填 parquet 或 textfile（注意：向 Hive 同步数据仅支持 orc/textfile，parquet 无法写入）  
"hiveDefaultFS":"", // 仅 CreateHiveProcess 使用：HDFS defaultFS，HA 场景填 nameservice（如 hdfs://nameservice1）；留空则自动取自 Hive 表 Location  
"hiveFieldDelimiter":"", // 仅 CreateHiveProcess 使用：hdfswriter 字段分隔符，留空默认 Hive 文本默认分隔符 （ORC 表忽略此项）  
"hiveHadoopConfig":{} // 仅 CreateHiveProcess 使用：hdfswriter 的 hadoopConfig，HA 场景填 dfs.nameservices/dfs.ha.namenodes.*/dfs.namenode.rpc-address.*/dfs.client.failover.proxy.provider.* 等；为空则省略  
}

1.不带参数为创建工作流

2.可以接CreateTable参数（不区分大小写），在去向数据库自动建表，含主键、唯一键等信息，如：  
java -jar dp_sync-1.jar CreateTable

3.可以接CreateTableNoKey参数（不区分大小写），在去向数据库自动建表，但不含主键、唯一键等信息，如：  
java -jar dp_sync-1.jar CreateTableNoKey

4.可以接一个projectCode参数，如果有参数，则下线该项目下所有工作流，方便后续删除，如：  
java -jar dp_sync-1.jar 17391796121440  

5.可以接CreateHiveTable参数（不区分大小写），根据 MySQL/PostgreSQL 源表结构在 Hive 中建表。input* 填源库，output* 填 Hive（jdbc:hive2://host:10000/库）。为避免复杂类型映射，Hive 列类型仅 bigint/double/string 三种；DDL 形如 库.表，库名取自 output URL 路径段；不含主键、唯一键等约束，如：  
java -jar dp_sync-1.jar CreateHiveTable  

6.可以接CreateHiveProcess参数（不区分大小写），创建"MySQL/PostgreSQL → Hive"的数据同步工作流（DataX hdfswriter 写 HDFS）。input* 填源库，output* 填 Hive。前提：目标 Hive 表须已存在（先跑 CreateHiveTable）且存储格式为 orc/textfile。HDFS 路径与 fileType 自动通过 DESCRIBE FORMATTED 探测；HA 场景需填 hiveDefaultFS 与 hiveHadoopConfig。where/specified/deleteWhere/cycle 等可选配置语义同创建工作流，如：  
java -jar dp_sync-1.jar CreateHiveProcess  