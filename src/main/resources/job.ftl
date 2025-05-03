{
  "job": {
    "setting": {
      "errorLimit": {
        "record": ${errorLimit},
        "percentage": 0
      },
      "speed": {
        "byte": 8388608,
        "channel": "1"
      }
    },
    "content": [
      {
        "reader": {
          "name": "${readerType}",
          "parameter": {
            "username": "${inputPgUserName}",
            "password": "${inputPgPassword}",
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
          "name": "${writerType}",
          "parameter": {
            "username": "${outputPgUserName}",
            "password": "${outputPgPassword}",
            "column": [
              ${outputColumn}
            ],
            "preSql": [
              "${preSql1}"
            ],
            "postSql": [
            ],
            "connection": [
              {
                "jdbcUrl": "${outputJdbcUrl}",
                "table": [
                  "${table}"
                ]
              }
            ]
          }
        }
      }
    ]
  }
}