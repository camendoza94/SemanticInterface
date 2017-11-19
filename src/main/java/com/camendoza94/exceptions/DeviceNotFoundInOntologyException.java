package com.camendoza94.exceptions;

public class DeviceNotFoundInOntologyException extends Exception {
    public DeviceNotFoundInOntologyException() {
        super("Device was not found in ontology.");
    }
}
