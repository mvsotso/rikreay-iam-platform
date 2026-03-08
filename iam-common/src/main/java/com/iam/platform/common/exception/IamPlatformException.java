package com.iam.platform.common.exception;

public class IamPlatformException extends RuntimeException {

    public IamPlatformException(String message) {
        super(message);
    }

    public IamPlatformException(String message, Throwable cause) {
        super(message, cause);
    }
}
