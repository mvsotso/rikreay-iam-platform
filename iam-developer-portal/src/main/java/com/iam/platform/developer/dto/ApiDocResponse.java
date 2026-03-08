package com.iam.platform.developer.dto;

import java.util.Map;

public record ApiDocResponse(
    String serviceName,
    String serviceUrl,
    Map<String, Object> openApiSpec
) {}
