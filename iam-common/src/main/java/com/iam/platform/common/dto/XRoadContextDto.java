package com.iam.platform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XRoadContextDto {

    private String clientInstance;
    private String clientMemberClass;
    private String clientMemberCode;
    private String clientSubsystem;
    private String fullClientId;
    private String messageId;
    private String userId;
    private String requestHash;
    private String serviceId;
    private long requestTimestamp;

    public String getMemberId() {
        return clientInstance + "/" + clientMemberClass + "/" + clientMemberCode;
    }

    public boolean isGovernmentRequest() {
        return "GOV".equalsIgnoreCase(clientMemberClass);
    }
}
