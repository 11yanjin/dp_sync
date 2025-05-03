{  
"dpHttpUrl": "http://192.168.10.11:12345",  
"dpToken": "139c4d43544b5b3f946cf1632de8eb0f",  
"dpTenant": "yanjin",  
"dpProjectName":"数据同步3",  
"dpProjectCode":"",  
"inputJdbcUrl": "jdbc:postgresql://192.168.10.12:5432/pgtest",  
"inputPgUserName":"postgres",  
"inputPgPassword":"postgres",  
"outputJdbcUrl": "jdbc:mysql://192.168.10.11:3306/test?characterEncoding=UTF-8",  
"outputPgUserName":"root",  
"outputPgPassword":"root",  
"errorLimit":0, //脏数据条数最大值，主键或唯一键重复即脏数据   
"specified":"99 as id,'张三' as name", //直接指定值字段，用逗号分隔，形式为xx as xx,xx as xx  
"where":"sex='男'", //来源表过滤条件，不填则全部同步  
"deleteWhere":"1=1", //同步前去向表删除条件：值为"truncate"或"1=1"，则清空表；为空字符串""或"1=2"则不删除任何数据；为其他内容则作为删除过滤条件  
"cycle":"minute:15", //调度周期，不填默认为每天，"minute:15"代表每15分钟执行一次，"hour:2"代表每2小时执行一次  
"hourBegin":0,  
"minuteBegin":10,  
"tableDispatchBatchSize":2,  
"tableDispatchBatchInterval":3,  
"executeOnce":"yes", //值为"yes"或"true"则立即执行一次，否则不执行  
"prefix":"o_full_", //去向表的前缀  
"tables":"test1,ff,table1,sys_user"  
}

可以接一个projectCode参数，如果有参数，则下线该项目下所有工作流，方便后续删除，如：
java -jar dp_pg_sync-1.jar 17391796121440
