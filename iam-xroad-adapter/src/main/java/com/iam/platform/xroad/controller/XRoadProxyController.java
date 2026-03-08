package com.iam.platform.xroad.controller;

import com.iam.platform.common.constants.XRoadHeaders;
import com.iam.platform.common.dto.XRoadContextDto;
import com.iam.platform.common.filter.XRoadRequestFilter;
import com.iam.platform.xroad.service.XRoadRoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catch-all proxy controller for X-Road Security Server requests.
 * Authenticated by X-Road Security Server (not JWT) — /xroad/** is permitAll in SecurityConfig.
 * Uses ThreadLocal XRoadContextDto populated by XRoadRequestFilter.
 */
@Slf4j
@RestController
@RequestMapping("/xroad")
@RequiredArgsConstructor
@Tag(name = "X-Road Proxy", description = "X-Road Security Server request proxy")
public class XRoadProxyController {

    private final XRoadRoutingService routingService;

    @RequestMapping(
            value = "/{serviceCode}/**",
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Proxy X-Road request",
            description = "Routes X-Road request to internal service based on service code and ACL validation")
    public ResponseEntity<String> proxyRequest(
            @PathVariable String serviceCode,
            @RequestBody(required = false) String requestBody,
            HttpServletRequest request) {

        XRoadContextDto context = XRoadRequestFilter.getContext();
        if (context == null) {
            return ResponseEntity.badRequest()
                    .body("{\"success\":false,\"message\":\"X-Road context not available\"}");
        }

        // Extract path suffix after /xroad/{serviceCode}/
        String fullPath = request.getRequestURI();
        String prefix = "/xroad/" + serviceCode;
        String pathSuffix = fullPath.length() > prefix.length()
                ? fullPath.substring(prefix.length() + 1) : "";

        String response = routingService.routeRequest(serviceCode, pathSuffix, context, requestBody);

        return ResponseEntity.ok()
                .header(XRoadHeaders.ID, context.getMessageId())
                .body(response);
    }
}
