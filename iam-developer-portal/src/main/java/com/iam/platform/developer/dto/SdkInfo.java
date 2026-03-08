package com.iam.platform.developer.dto;

public record SdkInfo(
    String language,
    String version,
    String downloadUrl,
    String documentationUrl
) {}
