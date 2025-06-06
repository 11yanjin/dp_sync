import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import constant.Constant;
import dolphinscheduler.DolphinSchedulerTool;
import entity.DPProcessDefinition;
import entity.DPProject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateProcess {
    public static void execute(JSONObject config) {
        String inputJdbcUrl = config.getStr("inputJdbcUrl");
        String inputUserName = config.getStr("inputUserName");
        String inputPassword = config.getStr("inputPassword");
        String outputJdbcUrl = config.getStr("outputJdbcUrl");
        String outputUserName = config.getStr("outputUserName");
        String outputPassword = config.getStr("outputPassword");
        int errorLimit = config.getInt("errorLimit");
        String specified = config.getStr("specified");
        String where = config.getStr("where");
        String deleteWhere = config.getStr("deleteWhere");
        String cycle = config.getStr("cycle");
        int hourBegin = config.getInt("hourBegin");
        int minuteBegin = config.getInt("minuteBegin");
        int tableDispatchBatchSize = config.getInt("tableDispatchBatchSize");
        int tableDispatchBatchInterval = config.getInt("tableDispatchBatchInterval");
        String executeOnce = config.getStr("executeOnce");
        String prefix = config.getStr("prefix");
        String tables = config.getStr("tables");
        // 海豚调度其工作类
        String dpHttpUrl = config.getStr("dpHttpUrl");
        String dpToken = config.getStr("dpToken");
        String dpTenant = config.getStr("dpTenant");
        DolphinSchedulerTool dolphinSchedulerTool = new DolphinSchedulerTool(dpHttpUrl, dpToken);
        //创建海豚调度器中的项目
        String dpProjectCode;
        String dpProjectName = config.getStr("dpProjectName");
        if (StrUtil.isNotBlank(config.getStr("dpProjectCode"))) {
            dpProjectCode = config.getStr("dpProjectCode");
        } else {
            DPProject dpProject = new DPProject();
            dpProject.setProjectName(dpProjectName);
            dpProjectCode = dolphinSchedulerTool.createProject(dpProject);
        }
        String[] split1 = tables.split(",");
        String[] split2 = specified.split(",");//切分 直接指定值字段
        Map<String, String> fieldMapping = new HashMap<>();
        for (String expr : split2) {
            expr = expr.trim();
            String[] parts = expr.split("\\s+[Aa][Ss]\\s+", 2);
            if (parts.length == 2) {
                String value = parts[0].trim();
                String fieldName = parts[1].trim().toLowerCase();
                fieldMapping.put(fieldName, value);
            }
        }
        List<String> taskCodeList = dolphinSchedulerTool.getTaskCode(dpProjectCode, split1.length);
        Iterator<String> taskCodeIterator = taskCodeList.iterator();
        int currentTableNum = 0;
        for (String tableName : split1) {
            List<String> tableFields = getTableFields(inputJdbcUrl, inputUserName, inputPassword, tableName);
            if (tableFields.isEmpty()) {
                continue;
            }
            List<String> columnList4In = new ArrayList<>();
            List<String> columnList4Out = new ArrayList<>();
            for (String item : tableFields) {
                String lowerItem = item.toLowerCase();
                if (fieldMapping.containsKey(lowerItem)) {
                    columnList4In.add(fieldMapping.get(lowerItem) + " as " + item);
                } else {
                    columnList4In.add(item);
                }
                columnList4Out.add("\"" + item + "\"");
            }
            String OutTableName = prefix + tableName;
            String preSql1;
            if (deleteWhere.equals("truncate") || deleteWhere.equals("1=1")) {
                preSql1 = "truncate table " + OutTableName;
            } else if (deleteWhere.isEmpty() || deleteWhere.equals("1=2")) {
                preSql1 = null;
            } else {
                preSql1 = "delete from " + OutTableName + " where " + deleteWhere;
            }
            //String postSql1 = "vacuum full " + tableName;
            String querySql = "select " + StrUtil.join(",", columnList4In) + " from " + tableName + (StrUtil.isEmpty(where) ? "" : " where " + where);
            String outputColumn = StrUtil.join(",", columnList4Out);
            //获取数据源类型
            Pattern pattern = Pattern.compile("jdbc:([^:]+):");
            Matcher readerMatcher = pattern.matcher(inputJdbcUrl);
            String readerType = "";
            String writerType = "";
            if (readerMatcher.find()) {
                readerType = readerMatcher.group(1) + "reader";
            }
            Matcher writerMatcher = pattern.matcher(outputJdbcUrl);
            if (writerMatcher.find()) {
                writerType = writerMatcher.group(1) + "writer";
            }
            // 拼接param
            Map<String, Object> param = new HashMap<>();
            param.put("readerType", readerType);
            param.put("inputUserName", inputUserName);
            param.put("inputPassword", inputPassword);
            param.put("querySql", querySql);
            param.put("inputJdbcUrl", inputJdbcUrl);
            param.put("writerType", writerType);
            param.put("outputUserName", outputUserName);
            param.put("outputPassword", outputPassword);
            param.put("outputColumn", outputColumn);
            param.put("preSql1", preSql1);
            //param.put("postSql1", postSql1);
            param.put("outputJdbcUrl", outputJdbcUrl);
            param.put("table", OutTableName);
            param.put("errorLimit", errorLimit);

            TemplateEngine engine = TemplateUtil.createEngine(new TemplateConfig("", TemplateConfig.ResourceMode.CLASSPATH));
            Template template = engine.getTemplate("job.ftl");
            String jobConfig = template.render(param);

            //创建海豚调度器工作流，并配置定时上线
            String taskCode = taskCodeIterator.next();
            DPProcessDefinition dpProcessDefinition = new DPProcessDefinition(dpTenant);
            dpProcessDefinition.setName(tableName);
            dpProcessDefinition.setLocations(new JSONArray(Constant.locations.replace(Constant.taskCode, taskCode)).toString());
            dpProcessDefinition.setTaskRelationJson(new JSONArray(Constant.taskRelationJson.replace(Constant.taskCode, taskCode)).toString());
            JSONArray jsonArray = new JSONArray(Constant.taskDefinitionJson);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            jsonObject.set("code", taskCode);
            jsonObject.getJSONObject("taskParams").set("json", jobConfig);
            dpProcessDefinition.setTaskDefinitionJson(jsonArray.toString());
            dolphinSchedulerTool.createProcessDefinition(dpProjectCode, dpProcessDefinition);
            dolphinSchedulerTool.release(dpProjectCode, dpProcessDefinition.getCode(), "ONLINE");
            String schedule_id = dolphinSchedulerTool.createSchedule(dpProjectCode, dpProcessDefinition.getCode(),
                    getCron(hourBegin, minuteBegin, tableDispatchBatchSize, tableDispatchBatchInterval, currentTableNum, cycle), null);
            dolphinSchedulerTool.releaseSchedule(dpProjectCode, schedule_id, "online");
            //将工作流立即执行一次
            if (executeOnce.equalsIgnoreCase("yes") || executeOnce.equalsIgnoreCase("true")) {
                dolphinSchedulerTool.executeOnce(dpProjectCode, dpProcessDefinition.getCode());
            }
            currentTableNum++;
            System.out.println("第" + currentTableNum + "张表:" + tableName + "已同步");
        }

    }

    private static String getCron(int hourBegin, int minuteBegin, int tableDispatchBatchSize, int tableDispatchBatchInterval, int currentTableNum, String cycle) {
        String cron = "0 a b * * ? *";
        int total = (currentTableNum / tableDispatchBatchSize) * tableDispatchBatchInterval;
        int hour = (total / 60) + hourBegin;
        int minute = (total % 60) + minuteBegin;
        if (cycle.contains("hour")) {
            String hourCycle = cycle.substring("hour:".length());
            return cron.replace("a", minute + "").replace("b", hour + "/" + hourCycle);
        } else if (cycle.contains("minute")) {
            String minuteCycle = cycle.substring("minute:".length());
            return cron.replace("a", minute + "/" + minuteCycle).replace("b", hour + "/1");
        }
        return cron.replace("a", minute + "").replace("b", hour + "");
    }

    private static List<String> getTableFields(String jdbcUrl, String userName, String password, String tableName) {
        List<String> ret = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, userName, password);
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery("select * from " + tableName + " where 1=2")) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                ret.add(metaData.getColumnName(i));
            }
        } catch (SQLException e) {
            String state = e.getSQLState();
            if ("42P01".equals(state) || "42S02".equals(state)) { // 42P01:postgresql 42S02:MySQL
                System.err.println("表 " + tableName + " 不存在，跳过");
            } else {
                throw new RuntimeException(e);
            }
        }
        return ret;
    }
}