package com.uniovi.estimacion.services.sizeanalyses.functionpoints;

public class InvalidFunctionPointXmlException extends RuntimeException {

    public InvalidFunctionPointXmlException(String message) {
        super(message);
    }

    public InvalidFunctionPointXmlException(String message, Throwable cause) {
        super(message, cause);
    }
}
