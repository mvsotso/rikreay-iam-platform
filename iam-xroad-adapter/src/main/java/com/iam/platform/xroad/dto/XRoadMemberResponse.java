package com.iam.platform.xroad.dto;

public record XRoadMemberResponse(
        String instance,
        String memberClass,
        String memberCode,
        String subsystem,
        String fullIdentifier
) {}
