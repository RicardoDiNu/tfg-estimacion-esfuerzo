package com.uniovi.estimacion.services.sizeanalyses.usecasepoints;

public class InvalidUseCasePointXmlException extends RuntimeException {

    public InvalidUseCasePointXmlException(String message) {
        super(message);
    }

    public InvalidUseCasePointXmlException(String message, Throwable cause) {
        super(message, cause);
    }
}
