package com.camendoza94.semanticinterface;


import org.springframework.data.annotation.Id;

import java.util.Date;

public class Measurement {

    @Id
    private String id;

    private double value;

    private Date timestamp;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
