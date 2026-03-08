package com.iam.platform.common.filter;

import com.iam.platform.common.constants.XRoadHeaders;
import com.iam.platform.common.dto.XRoadContextDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that extracts X-Road headers into XRoadContextDto and stores in ThreadLocal.
 * <p>
 * Only processes /xroad/** requests. Authenticated by X-Road Security Server, NOT by JWT.
 * <p>
 * IMPORTANT: This filter uses ThreadLocal — it must NOT be used in reactive services (iam-gateway).
 */
@Slf4j
public class XRoadRequestFilter extends OncePerRequestFilter {

    private static final String XROAD_PATH_PREFIX = "/xroad/";
    private static final ThreadLocal<XRoadContextDto> XROAD_CONTEXT = new ThreadLocal<>();

    @Value("${xroad.enabled:true}")
    private boolean xroadEnabled;

    public static XRoadContextDto getContext() {
        return XROAD_CONTEXT.get();
    }

    public static boolean isXRoadRequest() {
        return XROAD_CONTEXT.get() != null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(XROAD_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (!xroadEnabled) {
                log.warn("X-Road is disabled, rejecting request: {}", request.getRequestURI());
                sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "X-Road service is currently disabled");
                return;
            }

            String clientHeader = request.getHeader(XRoadHeaders.CLIENT);
            String messageId = request.getHeader(XRoadHeaders.ID);

            if (clientHeader == null || clientHeader.isBlank()) {
                log.warn("Missing required X-Road-Client header");
                sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Missing required header: " + XRoadHeaders.CLIENT);
                return;
            }

            if (messageId == null || messageId.isBlank()) {
                log.warn("Missing required X-Road-Id header");
                sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Missing required header: " + XRoadHeaders.ID);
                return;
            }

            XRoadContextDto context = parseClientHeader(clientHeader, messageId, request);
            if (context == null) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid X-Road-Client header format. Expected: INSTANCE/CLASS/MEMBER/SUBSYSTEM");
                return;
            }

            XROAD_CONTEXT.set(context);
            response.setHeader(XRoadHeaders.ID, messageId);

            log.debug("X-Road request from {} [messageId={}]", context.getFullClientId(), messageId);
            filterChain.doFilter(request, response);

        } finally {
            XROAD_CONTEXT.remove();
        }
    }

    private XRoadContextDto parseClientHeader(String clientHeader, String messageId,
                                               HttpServletRequest request) {
        String[] parts = clientHeader.split("/");
        if (parts.length < 4) {
            log.warn("Invalid X-Road-Client header format: {}", clientHeader);
            return null;
        }

        return XRoadContextDto.builder()
                .clientInstance(parts[0])
                .clientMemberClass(parts[1])
                .clientMemberCode(parts[2])
                .clientSubsystem(parts[3])
                .fullClientId(clientHeader)
                .messageId(messageId)
                .userId(request.getHeader(XRoadHeaders.USER_ID))
                .requestHash(request.getHeader(XRoadHeaders.REQUEST_HASH))
                .serviceId(request.getHeader(XRoadHeaders.SERVICE))
                .requestTimestamp(System.currentTimeMillis())
                .build();
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"" + message + "\",\"errorCode\":\"XROAD_ERROR\"}"
        );
    }
}
