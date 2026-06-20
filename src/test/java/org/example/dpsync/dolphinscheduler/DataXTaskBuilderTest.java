package org.example.dpsync.dolphinscheduler;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link DataXTaskBuilder} 生成的三段 JSON 结构正确，
 * 且 taskCode 已正确写入（取代原先 Constant 字符串 replace 的脆弱实现）。
 */
class DataXTaskBuilderTest {

    private static final String TASK_CODE = "6345052619968";

    @Test
    void buildLocations_containsTaskCodeAndCoordinates() {
        JSONArray arr = new JSONArray(DataXTaskBuilder.buildLocations(TASK_CODE));
        JSONObject node = arr.getJSONObject(0);
        assertEquals(Long.parseLong(TASK_CODE), node.getLong("taskCode"));
        assertEquals(180, node.getInt("x"));
        assertEquals(137, node.getInt("y"));
    }

    @Test
    void buildTaskRelationJson_pointsPostTaskToSelf() {
        JSONArray arr = new JSONArray(DataXTaskBuilder.buildTaskRelationJson(TASK_CODE));
        JSONObject rel = arr.getJSONObject(0);
        assertEquals(0, rel.getInt("preTaskCode"));
        assertEquals(Long.parseLong(TASK_CODE), rel.getLong("postTaskCode"));
    }

    @Test
    void buildTaskDefinitionJson_embedsJobConfigAndTaskCode() {
        String jobConfig = "{\"job\":\"x\"}";
        JSONArray arr = new JSONArray(DataXTaskBuilder.buildTaskDefinitionJson(TASK_CODE, jobConfig));
        JSONObject task = arr.getJSONObject(0);
        assertEquals(TASK_CODE, task.getStr("code"));
        assertEquals("DATAX", task.getStr("taskType"));
        assertEquals(jobConfig, task.getJSONObject("taskParams").getStr("json"));
    }

    @Test
    void buildTaskDefinitionJson_doesNotLeakPlaceholderCode() {
        // 旧实现里有固定占位 code 6345052619968，换 taskCode 后不应残留默认占位之外的串
        String json = DataXTaskBuilder.buildTaskDefinitionJson("123", "{}");
        assertTrue(json.contains("\"code\":\"123\""));
        assertFalse(json.contains("6345052619968"));
    }
}
