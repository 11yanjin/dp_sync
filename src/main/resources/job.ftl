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
          "name": "${writerType}",
          "parameter": {
            "username": "${outputUserName}",
            "password": "${outputPassword}",
            "column": [
              ${outputColumn}
            ],
            "preSql": [
              <#if preSql1??>"${preSql1}"</#if>
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