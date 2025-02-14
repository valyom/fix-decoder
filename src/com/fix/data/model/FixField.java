package com.fix.data.model;

import java.util.HashMap;
import java.util.Map;
public class FixField {
    private String number;
    private String name;
    private String type;

    Map<String, String> values = new HashMap<>();

    public FixField(String number, String name, String type) {
        this.number = number;
        this.name = name;
        this.type = type;
    }

    public String getValueDescription (String value) {
        String description = values.get(value);
        if (description != null)
            return description;

        try {
            int  i = Integer.parseInt(value);
            String newKey =  "" + (char)('A'  + (i - 11));
            description = values.get(newKey);

            if (description != null)
                return description;
        } catch (NumberFormatException ignore) {}

        return value;
    }

    public void addValue (String value, String description) {
        if (value != null)
            values.put(value, description != null ? description : "null");
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "FixField{" +
                "number='" + number + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", values=" + values +
                '}';
    }
}
