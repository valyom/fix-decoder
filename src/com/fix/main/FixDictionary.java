package com.fix.main;

import com.fix.data.model.FixField;
import com.fix.parser.FieldHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;
import java.io.InputStream;

public class FixDictionary {
    private static final String DEFAULT_XML_DICTIONARY = "/dict1.xml";
    private static final String DEFAULT_XML_DICTIONARY_DEBUG = "/home/victor/ws/fix-decoder/data/dict1.xml";
    private Map<String, FixField> fields = null;

    public boolean isInitialized() {
        return fields != null && !fields.isEmpty();
    }
    public void init() {
        initProd();
    }

    public void initDebug() {
        String dictFile = DEFAULT_XML_DICTIONARY_DEBUG;
        try {
            File inputFile = new File(dictFile);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            FieldHandler handler = new FieldHandler();
            saxParser.parse(inputFile, handler);
            fields = handler.getFieldList();
        } catch ( ParserConfigurationException | SAXException | IOException  e) {
            e.printStackTrace();
        }
    }
    public void initProd() {
        String dictFile = DEFAULT_XML_DICTIONARY;
        try (InputStream inputStream = getClass().getResourceAsStream(dictFile)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found: " + dictFile);
            }

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            FieldHandler handler = new FieldHandler();
            saxParser.parse(new InputSource(inputStream), handler); // Use InputSource for InputStream
            fields = handler.getFieldList();
        } catch (IOException | SAXException | javax.xml.parsers.ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public String explainMessage(String message) {
        if (fields == null)
            return message;

        String[] parts = message.split("\\|");
        String[] translated = new String[parts.length];
        int j = 0;
        for (String part : parts) {
            String[] pair = part.split("=");
            FixField f = fields.get(pair[0]);
            if (f == null) {
                translated[j++] = part;
                continue;
            }

            //System.out.print(f.getName() + "=" + f.getValueDescription(pair[1]) );
            String description = f.getValueDescription(pair[1]);
            String traslatedPart = f.getName() + "(" + pair[0] + ")" + "=" + description;
            if (pair[1].compareTo(description) != 0)  {
                traslatedPart = traslatedPart + "(" + pair[1] + ")";
            }
            translated[j++] = traslatedPart ;
        }

        // return "\n\n[" + String.join(" |\n\t ", translated) + "]";

        return "\n[" + String.join(" \n\t ", translated) + "]";
    }

    private FileReader  getInputStream (String file)  {
        try {
            return new FileReader(file);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }
}
