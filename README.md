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
"executeOnce":"yes", // 值为"yes"或"true"则立即执行一次，否则不执行  
"prefix":"o_full_", // 去向表的前缀  
"tables":"test1,ff,table1,sys_user"  
}

1.不带参数为创建工作流

2.可以接CreateTable参数（不区分大小写），在去向数据库自动建表，含主键、唯一键等信息，如：  
java -jar dp_sync-1.jar CreateTable

3.可以接CreateTableNoKey参数（不区分大小写），在去向数据库自动建表，但不含主键、唯一键等信息，如：  
java -jar dp_sync-1.jar CreateTableNoKey

4.可以接一个projectCode参数，如果有参数，则下线该项目下所有工作流，方便后续删除，如：  
java -jar dp_sync-1.jar 17391796121440  