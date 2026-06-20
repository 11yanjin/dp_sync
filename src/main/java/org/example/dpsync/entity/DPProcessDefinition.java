package org.example.dpsync.entity;

import lombok.Data;

@Data
public class DPProcessDefinition {
    String locations;
    String name;
    String taskDefinitionJson;
    String taskRelationJson;
    String tenantCode;
    String description = "";
    String globalParams = "[]";
    Integer timeout = 0;
    String releaseState = "OFFLINE";
    String executionType;
    String code;
    Integer version;

    public DPProcessDefinition(String tenantCode) {
        this.tenantCode = tenantCode;
    }
}
