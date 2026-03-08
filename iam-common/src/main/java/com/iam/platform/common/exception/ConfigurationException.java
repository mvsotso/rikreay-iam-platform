package com.iam.platform.common.exception;

public class ConfigurationException extends IamPlatformException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
