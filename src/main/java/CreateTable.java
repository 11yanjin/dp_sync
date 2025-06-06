import cn.hutool.json.JSONObject;
import entity.TableMetaData;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
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
            int i = 1;
            for (String tableName : split) {
                try {
                    TableMetaData tableMetaData = generateTableDDL(inputJdbcUrl, inputUserName, inputPassword, tableName, prefix, isRequiresKey);
                    StringBuilder ddl = tableMetaData.getDdl();
                    LinkedHashMap<String, String> Comments = tableMetaData.getComments();
                    String schema = tableMetaData.getSchema();
                    String actualTableName = tableMetaData.getActualTableName();
                    String finalDDL = ddlTransform(ddl, inputDatabaseType, outputDatabaseType);
                    System.out.printf("%d. %s%n", i++, finalDDL);
                    if (!finalDDL.contains("不存在")) {
                        stmt.executeUpdate(finalDDL);
                        System.out.println(prefix + tableName + "建表语句已执行");
                        if ("postgresql".equalsIgnoreCase(outputDatabaseType)) {
                            addComments(stmt, schema, prefix + actualTableName, Comments);
                        }
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
            throw new RuntimeException(e);
        }
    }

    public static String ddlTransform(StringBuilder ddl, String inputDatabaseType, String outputDatabaseType) {
        if ("mysql".equalsIgnoreCase(inputDatabaseType) && "mysql".equalsIgnoreCase(outputDatabaseType)) {
            return ddl.toString();
        } else if ("postgresql".equalsIgnoreCase(inputDatabaseType) && "postgresql".equalsIgnoreCase(outputDatabaseType)) {
            return ddl.toString()
                    .replaceAll(" COMMENT '(?:''|[^'])*'", "");
        } else if ("mysql".equalsIgnoreCase(inputDatabaseType) && "postgresql".equalsIgnoreCase(outputDatabaseType)) {
            return ddl.toString()
                    .replaceAll("\\bFLOAT\\b", "float4")
                    .replaceAll("\\bDOUBLE\\b", "float8")
                    .replaceAll("\\bDATETIME\\b", "timestamp")
                    .replaceAll("\\bTIMESTAMP\\b", "timestamptz")
                    .replaceAll("\\bTINYblob\\b", "bytea")
                    .replaceAll("\\bBLOB\\b", "bytea")
                    .replaceAll("\\bMEDIUMBLOB\\b", "bytea")
                    .replaceAll("\\bLONGBLOB\\b", "bytea")
                    .replaceAll(" COMMENT '(?:''|[^'])*'", "");
        } else if ("postgresql".equalsIgnoreCase(inputDatabaseType) && "mysql".equalsIgnoreCase(outputDatabaseType)) {
            return ddl.toString()
                    .replaceAll("\\bbpchar\\b", "char")
                    .replaceAll("\\btimestamp\\b", "datetime")
                    .replaceAll("\\btimestamptz\\b", "timestamp")
                    .replaceAll("\\bfloat4\\b", "float")
                    .replaceAll("\\bfloat8\\b", "double")
                    .replaceAll("\\bbytea\\b", "longblob")
                    .replaceAll("\\bjsonb\\b", "json");
        } else
            return ddl.toString();
    }

    public static TableMetaData generateTableDDL(String jdbcUrl, String userName, String password, String tableName, String prefix, boolean isRequiresKey) throws SQLException {
        String schema = null;
        String actualTableName = tableName; //用于分离tableName和schema
        // 解析schema和表名
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.", 2);
            schema = parts[0];
            actualTableName = parts[1];
        }
        Properties props = new Properties();
        props.setProperty("user", userName);
        props.setProperty("password", password);
        props.setProperty("useInformationSchema", "true");//默认为false，mysql需改为true才能获取表名注释
        try (Connection conn = DriverManager.getConnection(jdbcUrl, props)) {
            DatabaseMetaData metaData = conn.getMetaData();
            List<String> columns = new ArrayList<>();
            LinkedHashMap<String, String> Comments = new LinkedHashMap<>(); //postgresql需单独收集字段注释
            List<String> serialColumnName = new ArrayList<>();//mysql建表语句使用serial，无需手动加上UNIQUE，用于后面排除DDL的UNIQUE

            String databaseProductName = metaData.getDatabaseProductName();
            String tableComment;
            String catalog = conn.getCatalog(); // mysql的catalog即数据库名，需指定以避免查询到其他库
            try (ResultSet tableRs = metaData.getTables(catalog, schema, actualTableName, new String[]{"TABLE", "VIEW"})) {
                if (tableRs.next()) {
                    String remarks = tableRs.getString("REMARKS");//没有表名注释postgresql为null
                    tableComment = remarks != null ? remarks.replace("'", "''") : "";
                    Comments.put(actualTableName, tableComment);
                } else {
                    StringBuilder message = new StringBuilder("来源表").append(tableName).append("不存在");
                    return new TableMetaData(message, Comments, schema, actualTableName);
                }
            }
            try (ResultSet columnsRs = metaData.getColumns(catalog, schema, actualTableName, null)) {
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
                    // 收集注释信息，postgresql无注释时为null，mysql无注释时为''
                    if (remarks != null && !remarks.trim().isEmpty()) {
                        remarks = remarks.replace("'", "''");//输入时不接受注释内容有单独的'，可写成''，会转义为'
                        Comments.put(columnName, remarks);
                    }
                    if ("YES".equalsIgnoreCase(isAutoIncrement)) {
                        // mysql和postgresql的serial输出写法差异极大，但可用统一用serial输入
                        columnDef.append(columnName).append(" serial");
                        if (remarks != null && !remarks.isEmpty()) {
                            columnDef.append(" COMMENT '").append(remarks).append("'");
                        }
                        columns.add(columnDef.toString());
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
                    } else if (isTimeType(lowerType)) { // 时间系列字段长度处理
                        // 输入时，postgresql不写长度默认为6，mysql不写长度默认为0
                        if ("PostgreSQL".equalsIgnoreCase(databaseProductName)) {
                            columnDef.append("(").append(decimalDigits).append(")");
                        } else if ("MySQL".equalsIgnoreCase(databaseProductName)) {
                            int precision = calculateMySQLTimePrecision(lowerType, columnSize);
                            columnDef.append("(").append(precision).append(")");
                        }
                    } else if (typeRequiresLength(lowerType)) { // char系列字段长度处理
                        if (columnSize > 0) {
                            columnDef.append("(").append(columnSize).append(")");
                        }
                    }

                    if (nullable == DatabaseMetaData.columnNoNulls) {
                        columnDef.append(" NOT NULL");
                    }
                    if (defaultValue != null) {
                        if ("PostgreSQL".equalsIgnoreCase(databaseProductName)) {
                            int index = defaultValue.indexOf("::");// 去掉postgresql类型转换语法,因为套用在mysql会出现问题
                            if (index != -1) {
                                defaultValue = defaultValue.substring(0, index);
                            }
                            columnDef.append(" DEFAULT ").append(defaultValue);
                            if ("CURRENT_TIMESTAMP".equals(defaultValue)) {
                                columnDef.append("(").append(decimalDigits).append(")");
                            }
                        } else if ("MySQL".equalsIgnoreCase(databaseProductName)) {
                            if (typeRequiresLength(lowerType)) {
                                // mysql的char系列字段不带单引号
                                columnDef.append(" DEFAULT '").append(defaultValue).append("'");
                            } else
                                columnDef.append(" DEFAULT ").append(defaultValue);
                        }
                    }
                    if (remarks != null && !remarks.isEmpty()) {
                        columnDef.append(" COMMENT '").append(remarks).append("'");
                    }
                    columns.add(columnDef.toString());
                }
            }

            Map<Short, String> primaryKeyMap = new TreeMap<>();
            Set<String> primaryKeyNames = new HashSet<>();
            Map<String, TreeMap<Short, String>> uniqueConstraints = new LinkedHashMap<>();//唯一键的字段组
            if (isRequiresKey) {
                // 获取主键信息（按顺序）
                try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, schema, actualTableName)) {
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
                try (ResultSet indexRs = metaData.getIndexInfo(catalog, schema, actualTableName, false, false)) {
                    while (indexRs.next()) {
                        String indexName = indexRs.getString("INDEX_NAME");
                        boolean nonUnique = indexRs.getBoolean("NON_UNIQUE");
                        String columnName = indexRs.getString("COLUMN_NAME");
                        short seq = indexRs.getShort("ORDINAL_POSITION");
                        short type = indexRs.getShort("TYPE");//排除TYPE为0即tableIndexStatistic统计信息
                        // 排除主键索引和统计信息
                        if (primaryKeyNames.contains(indexName) || type == DatabaseMetaData.tableIndexStatistic || columnName == null) {
                            continue;
                        }
                        if (!nonUnique) {
                            uniqueConstraints.computeIfAbsent(indexName, k -> new TreeMap<>()).put(seq, columnName);
                        }
                    }
                }
                uniqueConstraints.entrySet().removeIf(entry -> {//mysql使用serial会自动带unique，排除DDL的UNIQUE
                    TreeMap<Short, String> keyColumnList = entry.getValue();
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
            if (!tableComment.isEmpty()) {
                ddl.append("\n) COMMENT '").append(tableComment).append("';");
            } else {
                ddl.append("\n);");
            }
            return new TableMetaData(ddl, Comments, schema, actualTableName);
        }
    }

    private static void addComments(Statement stmt, String schema, String tableName, LinkedHashMap<String, String> comments) throws SQLException {
        String fullTableName = (schema != null ? schema + "." : "") + tableName;
        // 获取并设置表注释
        if (!comments.isEmpty()) {
            String tableComment = comments.values().iterator().next();
            String tableCommentSql = String.format("COMMENT ON TABLE %s IS '%s';", fullTableName, tableComment);
            System.out.println(tableCommentSql);
            stmt.executeUpdate(tableCommentSql);
        }
        // 移除第一个元素（表注释）后处理列注释
        LinkedHashMap<String, String> columnComments = new LinkedHashMap<>(comments);
        if (!columnComments.isEmpty()) {
            columnComments.remove(columnComments.keySet().iterator().next());
        }
        for (Map.Entry<String, String> entry : columnComments.entrySet()) {
            String columnName = entry.getKey();
            String comment = entry.getValue();
            String commentSql = String.format("COMMENT ON COLUMN %s.%s IS '%s';", fullTableName, columnName, comment);
            System.out.println(commentSql);
            try {
                stmt.executeUpdate(commentSql);
            } catch (SQLException e) {
                System.err.printf("添加字段 %s 注释出错: %s%n", columnName, e.getMessage());
            }
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