package entity;

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
    String code;
    Integer version;

    public DPProcessDefinition(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    @Override
    public String toString() {
        return "DPProcessDefinition{" +
                "locations='" + locations + '\'' +
                ", name='" + name + '\'' +
                ", taskDefinitionJson='" + taskDefinitionJson + '\'' +
                ", taskRelationJson='" + taskRelationJson + '\'' +
                ", tenantCode='" + tenantCode + '\'' +
                ", description='" + description + '\'' +
                ", globalParams='" + globalParams + '\'' +
                ", timeout=" + timeout +
                ", releaseState='" + releaseState + '\'' +
                ", code='" + code + '\'' +
                ", version=" + version +
                '}';
    }
}
