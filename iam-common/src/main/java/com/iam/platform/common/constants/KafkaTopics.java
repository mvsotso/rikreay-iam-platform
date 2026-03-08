package com.iam.platform.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaTopics {

    public static final String AUDIT_EVENTS = "iam.audit.events";
    public static final String XROAD_EVENTS = "iam.xroad.events";
    public static final String NOTIFICATION_COMMANDS = "iam.notification.commands";
    public static final String PLATFORM_EVENTS = "iam.platform.events";
    public static final String ALERT_TRIGGERS = "iam.alert.triggers";
}
