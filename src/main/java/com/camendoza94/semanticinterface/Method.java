package com.camendoza94.semanticinterface;

import java.util.ArrayList;

class Method {
    private String URL;
    private ArrayList<String> fields;

    Method(String URL, ArrayList<String> fields) {
        this.URL = URL;
        this.fields = fields;
    }

    ArrayList<String> getFields() {
        return fields;
    }

    String getURL() {
        return URL;
    }
}
