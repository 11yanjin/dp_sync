import cn.hutool.json.JSONObject;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateTable {
    public static void execute(JSONObject config, boolean isRequiresKey) {
        String inputJdbcUrl = config.getStr("inputJdbcUrl");
        String inputUserName = config.getStr("inputUserName");
        String inputPassword = config.getStr("inputPassword");
        String outputJdbcUrl = config.getStr("outputJdbcUrl");
        String outputUserName = config.getStr("outputUserName");
        String outputPassword = config.getStr("outputPassword");
        String prefix = config.getStr("prefix");
        String tables = config.getStr("tables");
        String[] split = tables.split(",");
        //获取数据源类型
        Pattern pattern = Pattern.compile("jdbc:([^:]+):");
        String inputDatabaseType = "";
        String outputDatabaseType = "";
        Matcher inputMatcher = pattern.matcher(inputJdbcUrl);
        if (inputMatcher.find()) {
            inputDatabaseType = inputMatcher.group(1);
        }
        Matcher outputMatcher = pattern.matcher(outputJdbcUrl);
        if (outputMatcher.find()) {
            outputDatabaseType = outputMatcher.group(1);
        }
        try (Connection conn = DriverManager.getConnection(outputJdbcUrl, outputUserName, outputPassword);
             Statement stmt = conn.createStatement()) {
            for (String tableName : split) {
                try {
                    StringBuilder ddl = generateTableDDL(inputJdbcUrl, inputUserName, inputPassword,
                            tableName, prefix, isRequiresKey);
                    String finalDDL = ddlTransform(ddl, inputDatabaseType, outputDatabaseType);
                    System.out.println(ddl);
                    System.out.println("转换为");
                    System.out.println(finalDDL);
                    if (!finalDDL.contains("不存在")) {
                        int result = stmt.executeUpdate(finalDDL);
                        System.out.println(prefix + tableName + "建表语句已执行");
                        SQLWarning warning = stmt.getWarnings();
                        while (warning != null) {
                            System.out.println("注意: " + warning.getMessage());
                            warning = warning.getNextWarning();
                        }
                        stmt.clearWarnings(); // 清除当前警告
                    }
                } catch (SQLException e) {
                    System.err.println("创建表 " + prefix + tableName + " 时出错: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String ddlTransform(StringBuilder ddl, String inputDatabaseType, String outputDatabaseType) {
        if (inputDatabaseType.equals(outputDatabaseType)) {
            return ddl.toString();
        } else if ("mysql".equalsIgnoreCase(inputDatabaseType) && "postgresql".equalsIgnoreCase(outputDatabaseType)) {
            return ddl.toString()
                    .replaceAll("\\bFLOAT\\b", "float4")
                    .replaceAll("\\bDOUBLE\\b", "float8")
                    .replaceAll("\\bDATETIME\\b", "timestamp")
                    .replaceAll("\\bTIMESTAMP\\b", "timestamptz");
        } else if ("postgresql".equalsIgnoreCase(inputDatabaseType) && "mysql".equalsIgnoreCase(outputDatabaseType)) {
            return ddl.toString()
                    .replaceAll("\\bbpchar\\b", "char")
                    .replaceAll("\\btimestamp\\b", "datetime")
                    .replaceAll("\\btimestamptz\\b", "timestamp")
                    .replaceAll("\\bfloat4\\b", "float")
                    .replaceAll("\\bfloat8\\b", "double");
        } else
            return ddl.toString();
    }

    public static StringBuilder generateTableDDL(String jdbcUrl, String userName, String password,
                                                 String tableName,
                                                 String prefix, boolean isRequiresKey) throws SQLException {
        String schema = null;
        String actualTableName = tableName;

        // 解析schema和表名
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.", 2);
            schema = parts[0];
            actualTableName = parts[1];
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl, userName, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            List<String> columns = new ArrayList<>();
            List<String> serialColumnName = new ArrayList<>();//mysql建表语句使用serial，无需手动加上UNIQUE，用于后面排除DDL的UNIQUE

            String databaseProductName = metaData.getDatabaseProductName();
            try (ResultSet columnsRs = metaData.getColumns(null, schema, actualTableName, null)) {
                while (columnsRs.next()) {
                    String columnName = columnsRs.getString("COLUMN_NAME");
                    String typeName = columnsRs.getString("TYPE_NAME");
                    String remarks = columnsRs.getString("REMARKS");
                    int columnSize = columnsRs.getInt("COLUMN_SIZE");
                    int decimalDigits = columnsRs.getInt("DECIMAL_DIGITS");
                    int nullable = columnsRs.getInt("NULLABLE");
                    String defaultValue = columnsRs.getString("COLUMN_DEF");
                    String isAutoIncrement = columnsRs.getString("IS_AUTOINCREMENT");
                    StringBuilder columnDef = new StringBuilder();
                    String lowerType = typeName.toLowerCase();
                    if ("YES".equalsIgnoreCase(isAutoIncrement)) {
                        columnDef.append(columnName).append(" ").append("serial");//mysql和postgresql的serial输出写法差异极大，统一用serial
                        columns.add(columnDef.toString());//mysql的输出写法id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT不兼容postgresql
                        serialColumnName.add(columnName);
                        continue;
                    }
                    columnDef.append(columnName).append(" ").append(typeName);

                    if ("numeric".equals(lowerType) || "decimal".equals(lowerType)) {
                        if (columnSize > 0) {
                            columnDef.append("(").append(columnSize);
                            if (decimalDigits > 0) {
                                columnDef.append(",").append(decimalDigits);
                            }
                            columnDef.append(")");
                        }
                    } else if (isTimeType(lowerType)) { //时间系列字段长度处理
                        if ("PostgreSQL".equalsIgnoreCase(databaseProductName)) {
                            if (decimalDigits > 0) { //postgresql不写长度默认为6，mysql不写长度默认为0
                                columnDef.append("(").append(decimalDigits).append(")");
                            }
                        } else if ("MySQL".equalsIgnoreCase(databaseProductName)) {
                            int precision = calculateMySQLTimePrecision(lowerType, columnSize);
                            columnDef.append("(").append(precision).append(")");
                        }
                    } else if (typeRequiresLength(lowerType)) { //char系列字段长度处理
                        if (columnSize > 0) {
                            columnDef.append("(").append(columnSize).append(")");
                        }
                    }

                    if (nullable == DatabaseMetaData.columnNoNulls) {
                        columnDef.append(" NOT NULL");
                    }
                    if (defaultValue != null) {
                        if ("PostgreSQL".equalsIgnoreCase(databaseProductName)) {
                            int index = defaultValue.indexOf("::");//去掉postgresql类型转换语法,因为套用在mysql会出现问题
                            if (index != -1) {
                                defaultValue = defaultValue.substring(0, index);
                            }
                            columnDef.append(" DEFAULT ").append(defaultValue);
                        } else if ("MySQL".equalsIgnoreCase(databaseProductName)) {
                            if (typeRequiresLength(lowerType)) {
                                columnDef.append(" DEFAULT '").append(defaultValue).append("'");//mysql的char系列字段不带单引号
                            } else
                                columnDef.append(" DEFAULT ").append(defaultValue);
                        }
                    }
                    columns.add(columnDef.toString());
                }
                if (columns.isEmpty()) {
                    return new StringBuilder("来源表").append(tableName).append("不存在");
                }
            }

            Map<Short, String> primaryKeyMap = new TreeMap<>();
            Set<String> primaryKeyNames = new HashSet<>();
            Map<String, TreeMap<Short, String>> uniqueConstraints = new LinkedHashMap<>();//唯一键的字段组
            if (isRequiresKey) {
                // 获取主键信息（按顺序）
                try (ResultSet pkRs = metaData.getPrimaryKeys(null, schema, actualTableName)) {
                    while (pkRs.next()) {
                        String pkColumn = pkRs.getString("COLUMN_NAME");
                        short keySeq = pkRs.getShort("KEY_SEQ"); //键的字段的序号
                        primaryKeyMap.put(keySeq, pkColumn);
                        String pkName = pkRs.getString("PK_NAME");
                        if (pkName != null) {
                            primaryKeyNames.add(pkName);
                        }
                    }
                }
                // 获取唯一键信息
                try (ResultSet indexRs = metaData.getIndexInfo(null, schema, actualTableName, false, false)) {
                    while (indexRs.next()) {
                        String indexName = indexRs.getString("INDEX_NAME");
                        boolean nonUnique = indexRs.getBoolean("NON_UNIQUE");
                        String columnName = indexRs.getString("COLUMN_NAME");
                        short seq = indexRs.getShort("ORDINAL_POSITION");
                        short type = indexRs.getShort("TYPE");//排除TYPE为0即tableIndexStatistic统计信息

                        // 排除主键索引和统计信息
                        if (primaryKeyNames.contains(indexName)
                                || type == DatabaseMetaData.tableIndexStatistic || columnName == null) {
                            continue;
                        }
                        if (!nonUnique) {
                            uniqueConstraints.computeIfAbsent(indexName, k -> new TreeMap<>()).put(seq, columnName);
                        }
                    }
                }
                uniqueConstraints.entrySet().removeIf(entry -> {
                    TreeMap<Short, String> keyColumnList = entry.getValue();//mysql使用serial会自动带unique，再手动加unique就重复了
                    return keyColumnList.size() == 1 && serialColumnName.contains(keyColumnList.get((short) 1));

                });
            }
            // 构建DDL
            StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
            if (schema != null) {
                ddl.append(schema).append(".");
            }
            ddl.append(prefix).append(actualTableName).append("(\n");

            ddl.append(String.join(",\n", columns));// 添加列定义

            if (isRequiresKey) {
                if (!primaryKeyMap.isEmpty()) {// 添加主键约束
                    ddl.append(",\n\tPRIMARY KEY (");
                    ddl.append(String.join(", ", primaryKeyMap.values()));
                    ddl.append(")");
                }
                for (Map.Entry<String, TreeMap<Short, String>> entry : uniqueConstraints.entrySet()) {// 添加唯一键约束
                    ddl.append(",\n\t");
                    ddl.append("UNIQUE (").append(String.join(", ", entry.getValue().values())).append(")");
                }
            }
            ddl.append("\n);");
            return ddl;
        }
    }

    private static boolean isTimeType(String lowerType) {
        return lowerType.equals("timestamp")
                || lowerType.equals("time")
                || lowerType.equals("timestamptz") //postgresql
                || lowerType.equals("timetz") //postgresql
                || lowerType.equals("datetime"); //mysql
    }

    private static boolean typeRequiresLength(String lowerType) {
        return lowerType.startsWith("varchar")
                || lowerType.startsWith("char")
                || lowerType.startsWith("bpchar") //postgresql
                || lowerType.startsWith("varbit") //postgresql
                || lowerType.startsWith("bit")
                || lowerType.startsWith("varbinary") //mysql
                || lowerType.startsWith("binary"); //mysql
    }

    private static int calculateMySQLTimePrecision(String type, int columnSize) {
        int defaultLength;
        switch (type) {
            case "timestamp":
            case "datetime":
                defaultLength = 19;  // 默认长度：'YYYY-MM-DD HH:MM:SS'
                break;
            case "time":
                defaultLength = 8;   // 默认长度：'HH:MM:SS'
                break;
            default:
                return 0;
        }
        if (columnSize > defaultLength) {
            return (columnSize - defaultLength) - 1;
        }
        return 0;
    }
}