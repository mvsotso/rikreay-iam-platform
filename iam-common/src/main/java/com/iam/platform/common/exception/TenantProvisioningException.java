package com.iam.platform.common.exception;

public class TenantProvisioningException extends IamPlatformException {

    public TenantProvisioningException(String message) {
        super(message);
    }

    public TenantProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
