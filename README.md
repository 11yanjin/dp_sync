"dpHttpUrl":"http://192.168.1.14:12345",
"dpToken":"6eec01e12059ec50c63a43ad2364fc09",
"dpTenant": "luculent",
"dpProjectName":"数据同步3",
"dpProjectCode":"",
"inputPgHostPort":"192.168.1.13:5432",
"inputPgDataBase":"test1",
"inputPgUserName":"postgres",
"inputPgPassword":"luculent",
"outputPgHostPort":"192.168.1.13:5432",
"outputPgDataBase":"test2",
"outputPgUserName":"postgres",
"outputPgPassword":"luculent",
"errorLimit":0,
"specified":"99 as id,'66' as tenant_no",//直接指定值字段，用逗号分隔，形式为xx as xx,xx as xx
"where":"tenant_no = '15'",
"deleteWhere":"1=1",//值为"truncate"或"1=1"，则清空表；为空字符串""或"1=2"则不删除任何数据；为其他内容则作为删除过滤条件
"hourBegin":0,
"minuteBegin":10,
"tableDispatchBatchSize":2,
"tableDispatchBatchInterval":3,
"executeOnce":"yes",//值为"yes"或"true"则立即执行一次，否则不执行
"prefix":"o_full_",//目的端表的前缀
"tables":"test1,ff,table1,sys_user"

可以接一个projectCode参数，如果有参数，则下线该项目下所有工作流，方便后续删除，如：
java -jar dp_pg_sync-1.jar 17391796121440