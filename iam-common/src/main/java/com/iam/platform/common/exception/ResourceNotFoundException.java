package com.iam.platform.common.exception;

public class ResourceNotFoundException extends IamPlatformException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found: " + identifier);
    }
}
