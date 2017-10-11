package com.camendoza94.semanticinterface;

import org.springframework.data.annotation.Id;

import java.util.Date;

public class DeviceObservation {
    @Id
    private String id;

    //TODO change to String (JSON) to store more fields
    private double value;

    private Date timestamp;

    private String deviceId;

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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
