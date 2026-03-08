package com.iam.platform.common.exception;

public class IdentityVerificationException extends IamPlatformException {

    public IdentityVerificationException(String message) {
        super(message);
    }

    public IdentityVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
