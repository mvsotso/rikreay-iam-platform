package com.iam.platform.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class XRoadHeaders {

    public static final String CLIENT = "X-Road-Client";
    public static final String ID = "X-Road-Id";
    public static final String USER_ID = "X-Road-UserId";
    public static final String REQUEST_HASH = "X-Road-Request-Hash";
    public static final String SERVICE = "X-Road-Service";
    public static final String REPRESENTED_PARTY = "X-Road-Represented-Party";
}
