package com.rayx.modular.plugin.schemas;

import com.rayx.modular.plugin.schemas.type.YamlField;

import java.util.List;

public class YamlSDF {

    private String name;

    private String type;

    private List<YamlField> fields;

    private String code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<YamlField> getFields() {
        return fields;
    }

    public void setFields(List<YamlField> fields) {
        this.fields = fields;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
