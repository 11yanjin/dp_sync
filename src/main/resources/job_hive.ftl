{
  "job": {
    "setting": {
      "errorLimit": {
        "record": ${errorLimit},
        "percentage": 0
      },
      "speed": {
        "channel": "1"
      }
    },
    "content": [
      {
        "reader": {
          "name": "${readerType}",
          "parameter": {
            "username": "${inputUserName}",
            "password": "${inputPassword}",
            "where": "",
            "connection": [
              {
                "querySql": [
                  "${querySql}"
                ],
                "jdbcUrl": [
                  "${inputJdbcUrl}"
                ]
              }
            ]
          }
        },
        "writer": {
          "name": "hdfswriter",
          "parameter": {
            "defaultFS": "${defaultFS}",
            "fileType": "${fileType}",
            "path": "${path}",
            "fileName": "${fileName}",
            "writeMode": "${writeMode}",
            "fieldDelimiter": "${fieldDelimiter}",
            "column": ${hiveColumns}<#if hadoopConfig??>,
            "hadoopConfig": ${hadoopConfig}</#if>
          }
        }
      }
    ]
  }
}
