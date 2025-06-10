import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;

public class Main {
    public static void main(String[] args) {
        String basePath = System.getProperty("user.dir");
        String configPath = basePath + "/config.json";
        String configStr = FileUtil.readUtf8String(configPath);
        if (StrUtil.isBlank(configStr)) {
            throw new RuntimeException("读取配置文件失败，configPath:" + configPath);
        }
        JSONObject config = new JSONObject(configStr);
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("CreateTable")) {
                CreateTable.execute(config, true);
            } else if (args[0].equalsIgnoreCase("CreateTableNoKey")) {
                CreateTable.execute(config, false);
            } else if (StrUtil.isNumeric(args[0])) {
                OffLine.execute(config, args[0]);
            } else System.err.println("参数无效");
        } else
            CreateProcess.execute(config);
    }
}