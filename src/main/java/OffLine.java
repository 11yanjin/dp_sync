import cn.hutool.json.JSONObject;
import dolphinscheduler.DolphinSchedulerTool;

import java.util.Map;

public class OffLine {
    public static void execute(JSONObject config, String projectCode) {
        String dpHttpUrl = config.getStr("dpHttpUrl");
        String dpToken = config.getStr("dpToken");
        DolphinSchedulerTool dolphinSchedulerTool = new DolphinSchedulerTool(dpHttpUrl, dpToken);
        offLine(dolphinSchedulerTool, projectCode);
    }

    private static void offLine(DolphinSchedulerTool dolphinSchedulerTool, String projectCode) {
        Map<String, String> processList = dolphinSchedulerTool.queryList(projectCode);
        processList.forEach((code, name) -> {
            dolphinSchedulerTool.release(projectCode, code, "OFFLINE");
            System.out.println(name + "已下线");
        });
    }
}