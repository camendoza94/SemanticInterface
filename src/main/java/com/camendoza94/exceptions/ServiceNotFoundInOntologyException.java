package com.camendoza94.exceptions;

public class ServiceNotFoundInOntologyException extends Exception {
    public ServiceNotFoundInOntologyException() {
        super("Device was not found in ontology.");
    }
}
