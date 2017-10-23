package com.camendoza94.semanticinterface;

import java.util.ArrayList;

class Method {
    private String URL;
    private ArrayList<String> fields;
    private ArrayList<String> types;

    Method(String URL, ArrayList<String> fields, ArrayList<String> types) {
        this.URL = URL;
        this.fields = fields;
        this.types = types;
    }

    ArrayList<String> getFields() {
        return fields;
    }

    String getURL() {
        return URL;
    }

    public ArrayList<String> getTypes() {
        return types;
    }
}
