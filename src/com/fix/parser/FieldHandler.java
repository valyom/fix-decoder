package com.fix.parser;

import com.fix.data.model.FixField;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.LinkedHashMap;
import java.util.Map;

public class FieldHandler  extends DefaultHandler {
    private Map<String, FixField>  fieldList = new LinkedHashMap<>();
    private FixField currentField = null;

    public Map<String, FixField> getFieldList() {
        return fieldList;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("field".equals(qName)) {
            currentField = new FixField(
                    attributes.getValue("number"),
                    attributes.getValue("name"),
                    attributes.getValue("type")
            );
        } else if ("value".equals(qName) && currentField != null) {
            currentField.addValue(attributes.getValue("enum"), attributes.getValue("description"));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("field".equals(qName) && currentField != null) {
            fieldList.put(currentField.getNumber(), currentField);
            currentField = null;
        }
    }
}
