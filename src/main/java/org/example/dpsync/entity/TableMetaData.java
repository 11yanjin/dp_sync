package org.example.dpsync.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.LinkedHashMap;

@Data
@AllArgsConstructor
public class TableMetaData {
    private StringBuilder ddl;
    private LinkedHashMap<String, String> comments;
    private String schema;
    private String tableName;
}
