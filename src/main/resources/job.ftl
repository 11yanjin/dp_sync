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
          "name": "postgresqlreader",
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
          "name": "postgresqlwriter",
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