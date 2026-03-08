package com.iam.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCommandDto {

    private ChannelType channelType;
    private String recipient;
    private String subject;
    private String body;
    private String templateId;
    private Map<String, String> variables;
    private Priority priority;

    public enum ChannelType {
        EMAIL,
        SMS,
        TELEGRAM
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
}
