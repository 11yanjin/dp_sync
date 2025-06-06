package dolphinscheduler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import entity.DPDataSource;
import entity.DPProcessDefinition;
import entity.DPProject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DolphinSchedulerTool {
    private final String dpHttpUrl;
    private final String dpToken;
    private final int timeOut = 10000;

    public DolphinSchedulerTool(String dpHttpUrl, String dpToken) {
        this.dpHttpUrl = dpHttpUrl;
        this.dpToken = dpToken;
    }

    public Map<String, String> creatHeader() {
        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("token", this.dpToken);
        return httpHeaders;
    }

    public Map<String, String> creatHeader(String contentType) {
        Map<String, String> httpHeaders = this.creatHeader();
        httpHeaders.put("Content-Type", contentType);
        return httpHeaders;
    }

    public void checkRetCode(JSONObject retObj, String errorMsgPrefix) {
        Integer code = retObj.getInt("code");
        if (0 != code) {
            System.out.println(errorMsgPrefix + "失败！" + retObj);
            throw new RuntimeException(errorMsgPrefix + "失败！" + retObj.getStr("msg"));
        }
    }

    public String createProject(DPProject dpProject) {
        Map<String, Object> body = new HashMap<>();
        body.put("projectName", dpProject.getProjectName());
        Map<String, String> header = this.creatHeader("application/x-www-form-urlencoded");
        HttpResponse execute = (HttpUtil.createPost(this.dpHttpUrl + "/dolphinscheduler/projects").addHeaders(header)).timeout(this.timeOut).form(body).execute();
        JSONObject responseJson = new JSONObject(execute.body());
        this.checkRetCode(responseJson, "创建项目");
        return responseJson.getJSONObject("data").getStr("code");
    }

    public String createDataSource(DPDataSource dpDataSource) {
        Map<String, String> header = this.creatHeader("application/json");
        HttpResponse execute = (HttpUtil.createPost(this.dpHttpUrl + "/dolphinscheduler/datasources").addHeaders(header)).timeout(this.timeOut).body(new JSONObject(dpDataSource).toString()).execute();
        JSONObject responseJson = new JSONObject(execute.body());
        this.checkRetCode(responseJson, "创建数据源");
        int pageNo = 1;
        int pageSize = 1;
        String searchVal = dpDataSource.getName();
        HttpResponse executeSearch = (HttpUtil.createGet(this.dpHttpUrl + "/dolphinscheduler/datasources?pageNo=" + pageNo + "&pageSize=" + pageSize + "&searchVal=" + searchVal).addHeaders(this.creatHeader())).timeout(this.timeOut).execute();
        JSONObject queryResponseJson = new JSONObject(executeSearch.body());
        this.checkRetCode(queryResponseJson, "查询数据源编码");
        JSONObject queryData = queryResponseJson.getJSONObject("data");
        JSONArray totalList = queryData.getJSONArray("totalList");
        if (totalList.isEmpty()) {
            System.out.println("DolphinScheduler查询数据源ID失败！");
            throw new RuntimeException("DolphinScheduler查询数据源ID失败！");
        } else {
            return totalList.getJSONObject(0).getStr("id");
        }
    }

    public List<String> listWorker() {
        List<String> ret = new ArrayList<>();
        HttpResponse executeSearch = (HttpUtil.createGet(this.dpHttpUrl + "/dolphinscheduler/monitor/workers").addHeaders(this.creatHeader())).timeout(this.timeOut).execute();
        JSONObject dataRet = new JSONObject(executeSearch.body());
        this.checkRetCode(dataRet, "查询执行机器列表");
        JSONArray data = dataRet.getJSONArray("data");
        data.forEach((item) -> {
            JSONObject hostItem = (JSONObject) item;
            ret.add(hostItem.getStr("host"));
        });
        return ret;
    }

    public void createProcessDefinition(String projectCode, DPProcessDefinition dpProcessDefinition) {
        Map<String, String> header = this.creatHeader("application/x-www-form-urlencoded");
        HttpResponse execute = (HttpUtil.createPost(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/process-definition")
                .addHeaders(header)).timeout(this.timeOut).form(new JSONObject(dpProcessDefinition)).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "创建工作流");
        JSONObject data = dataRet.getJSONObject("data");
        String code = data.getStr("code");
        Integer version = data.getInt("version");
        dpProcessDefinition.setCode(code);
        dpProcessDefinition.setVersion(version);
    }

    public void updateProcessDefinition(String projectCode, String processDefinitionCode, DPProcessDefinition dpProcessDefinition) {
        Map<String, String> header = this.creatHeader("application/x-www-form-urlencoded");
        HttpResponse execute = (HttpUtil.createPost(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/process-definition/" + processDefinitionCode).addHeaders(header)).timeout(this.timeOut).form(new JSONObject(dpProcessDefinition)).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "更新工作流");
        JSONObject data = dataRet.getJSONObject("data");
        String code = data.getStr("code");
        Integer version = data.getInt("version");
        dpProcessDefinition.setCode(code);
        dpProcessDefinition.setVersion(version);
    }

    public List<String> getTaskCode(String projectCode, int genNum) {
        ArrayList<String> result = new ArrayList<>();
        int batch = (genNum / 100) + 1;
        for (int i = 1; i <= batch; i++) {
            int batchNum = 100;
            if (i == batch) {
                batchNum = genNum % 100;
            }
            HttpResponse executeSearch = (HttpUtil.createGet(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/task-definition/gen-task-codes?genNum=" + batchNum).addHeaders(this.creatHeader())).timeout(this.timeOut).execute();
            JSONObject dataRet = new JSONObject(executeSearch.body());
            this.checkRetCode(dataRet, "生成工作流坐标");
            JSONArray data = dataRet.getJSONArray("data");
            result.addAll(data.toList(String.class));
        }
        return result;
    }

    public void delProcessDefinitionVersion(String projectCode, String processDefinitionCode, String versionId) {
        Map<String, String> header = this.creatHeader();
        HttpResponse execute = (HttpUtil.createRequest(Method.DELETE, this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/process-definition/" + processDefinitionCode + "/versions/" + versionId).addHeaders(header)).timeout(this.timeOut).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "删除工作流版本");
    }

    public Map<String, String> queryList(String projectCode) {
        Map<String, String> processList = new HashMap<>();
        int pageNo = 1;
        while (true) {
            String url = this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/process-definition";
            url = url + "?pageSize=10&pageNo=" + pageNo;
            HttpResponse execute = HttpUtil.createGet(url).addHeaders(creatHeader()).timeout(this.timeOut).execute();
            JSONObject dataRet = new JSONObject(execute.body());
            this.checkRetCode(dataRet, "查询工作流定义列表");

            JSONObject data = dataRet.getJSONObject("data");
            if (data == null) {
                break;
            }
            JSONArray totalList = data.getJSONArray("totalList");
            if (totalList == null) {
                break;
            }
            for (Object item : totalList) {
                JSONObject workflow = (JSONObject) item;
                processList.put(workflow.getStr("code"), workflow.getStr("name"));
            }
            int totalPage = data.getInt("totalPage");
            if (pageNo >= totalPage) {
                break;
            }
            pageNo++;
        }
        return processList;
    }

    public int release(String projectCode, String processDefinitionCode, String releaseState) {
        JSONObject body = new JSONObject();
        body.set("releaseState", releaseState);
        Map<String, String> header = this.creatHeader("application/x-www-form-urlencoded");
        HttpResponse execute = (HttpUtil.createPost(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/process-definition/" + processDefinitionCode + "/release").addHeaders(header)).timeout(this.timeOut).form(body).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "生成工作流坐标");
        return dataRet.getInt("code");
    }

    public void executeOnce(String projectCode, String processDefinitionCode) {
        JSONObject body = new JSONObject();
        body.set("processDefinitionCode", processDefinitionCode);
        body.set("failureStrategy", "END");
        body.set("warningType", "NONE");
        body.set("execType", "START_PROCESS");
        body.set("taskDependType", "TASK_POST");
        body.set("complementDependentMode", "OFF_MODE");
        body.set("runMode", "RUN_MODE_SERIAL");
        body.set("processInstancePriority", "MEDIUM");
        body.set("workerGroup", "default");
        body.set("dryRun", 0);
        body.set("scheduleTime", "");
        Map<String, String> header = this.creatHeader("application/x-www-form-urlencoded");
        HttpResponse execute = HttpUtil.createPost(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/executors/start-process-instance")
                .addHeaders(header).timeout(this.timeOut).form(body).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "执行一次");
    }


    public String createSchedule(String projectCode, String processDefinitionCode, String cron, Date startTime) {
        JSONObject body = new JSONObject();
        body.set("schedule", this.getScheduleJson(cron, startTime).toString());
        body.set("failureStrategy", "CONTINUE");
        body.set("processInstancePriority", "MEDIUM");
        body.set("warningGroupId", "0");
        body.set("workerGroup", "default");
        body.set("warningType", "NONE");
        body.set("environmentCode", "");
        body.set("processDefinitionCode", processDefinitionCode);
        Map<String, String> header = this.creatHeader("application/x-www-form-urlencoded");
        HttpResponse execute = (HttpUtil.createPost(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/schedules").addHeaders(header)).timeout(this.timeOut).form(body).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "创建调度器");
        return dataRet.getJSONObject("data").getStr("id");
    }

    public String updateSchedule(String projectCode, String scheduleId, String cron, Date startTime) {
        JSONObject body = new JSONObject();
        body.set("schedule", this.getScheduleJson(cron, startTime).toString());
        body.set("failureStrategy", "CONTINUE");
        body.set("processInstancePriority", "MEDIUM");
        body.set("warningGroupId", "0");
        body.set("workerGroup", "default");
        body.set("warningType", "NONE");
        body.set("environmentCode", "");
        Map<String, String> header = this.creatHeader("application/x-www-form-urlencoded");
        HttpResponse execute = (HttpUtil.createRequest(Method.PUT, this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/schedules/" + scheduleId).addHeaders(header)).timeout(this.timeOut).form(body).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "更新调度器");
        return dataRet.getJSONObject("data").getStr("id");
    }

    public int releaseSchedule(String projectCode, String scheduleId, String releaseState) {
        Map<String, String> header = this.creatHeader("application/x-www-form-urlencoded");
        HttpResponse execute = (HttpUtil.createPost(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/schedules/" + scheduleId + "/" + releaseState).addHeaders(header)).timeout(this.timeOut).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "更新调度器");
        return dataRet.getInt("code");
    }

    private JSONObject getScheduleJson(String cron, Date startTime) {
        JSONObject json = new JSONObject(true);
        if (startTime == null) {
            json.set("startTime", DateUtil.formatDateTime(new Date()));
        } else {
            json.set("startTime", DateUtil.formatDateTime(startTime));
        }

        json.set("endTime", "2100-01-01 00:00:00");
        json.set("crontab", cron);
        json.set("timezoneId", "Asia/Shanghai");
        return json;
    }

    public JSONObject queryProcessInstanceListPaging(String projectCode, String processDefineCode, String page, String limit, String timeStart, String timeEnd, String status, String host) {
        HttpResponse execute = (HttpUtil.createGet(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/process-instances?pageNo=" + page + "&pageSize=" + limit + "&host=" + host + "&startDate=" + timeStart + "&endDate=" + timeEnd + "&status=" + status + "&processDefineCode=" + processDefineCode).addHeaders(this.creatHeader())).timeout(this.timeOut).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "更新调度器");
        return dataRet.getJSONObject("data");
    }

    public JSONObject queryAllProcessInstance(String projectCode, int pageSize) {
        HttpResponse execute = (HttpUtil.createGet(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/process-instances?searchVal=&pageSize=" + pageSize + "&pageNo=1&host=&stateType=&startDate=&endDate=&executorName=&_t=0.9923962552155072").addHeaders(this.creatHeader())).timeout(1500000).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "更新调度器");
        return dataRet.getJSONObject("data");
    }

    public void reRunInstance(String projectCode, String processInstanceId) {
        JSONObject body = new JSONObject();
        body.set("processInstanceId", processInstanceId);
        body.set("executeType", "REPEAT_RUNNING");
        Map<String, String> header = this.creatHeader("application/x-www-form-urlencoded");
        HttpResponse execute = (HttpUtil.createPost(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/executors/execute").addHeaders(header)).timeout(this.timeOut).form(body).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "工作流实例重跑");
    }

    public String getInstanceLog(int limit, long taskInstanceId) {
        HttpResponse execute = HttpUtil.createGet(this.dpHttpUrl + "/dolphinscheduler/log/detail?taskInstanceId=" + taskInstanceId + "&skipLineNum=0&limit=" + limit + "&_t=0.1011358573480663").addHeaders(this.creatHeader()).timeout(this.timeOut).execute();
        JSONObject returnJson = new JSONObject(execute.body());
        checkRetCode(returnJson, "获取任务实例日志详情出错！");
        return returnJson.getStr("data");
    }

    public JSONObject getDPDispatchInstanceDetail(String projectCode, String instanceId) {
        HttpResponse execute = (HttpUtil.createGet(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/process-instances/" + instanceId + "/tasks?processInstanceId=" + instanceId).addHeaders(this.creatHeader())).timeout(this.timeOut).execute();
        JSONObject returnJson = new JSONObject(execute.body());
        checkRetCode(returnJson, "获取工作流实例详情出错！");
        return returnJson.getJSONObject("data");
    }

    public void delProcessInstances(String projectCode, String[] processInstanceIds) {
        JSONObject body = new JSONObject();
        body.set("processInstanceIds", String.join(",", processInstanceIds));
        Map<String, String> header = this.creatHeader("application/x-www-form-urlencoded");
        HttpResponse execute = (HttpUtil.createPost(this.dpHttpUrl + "/dolphinscheduler/projects/" + projectCode + "/process-instances/batch-delete").addHeaders(header)).timeout(this.timeOut).form(body).execute();
        JSONObject dataRet = new JSONObject(execute.body());
        this.checkRetCode(dataRet, "删除工作流实例");
    }
}
