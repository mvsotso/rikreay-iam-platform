package com.iam.platform.common.filter;

import com.iam.platform.common.constants.XRoadHeaders;
import com.iam.platform.common.dto.XRoadContextDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class XRoadRequestFilterTest {

    private XRoadRequestFilter filter;

    @BeforeEach
    void setUp() {
        filter = new XRoadRequestFilter();
        ReflectionTestUtils.setField(filter, "xroadEnabled", true);
    }

    @Test
    @DisplayName("Should populate ThreadLocal with valid X-Road headers")
    void shouldPopulateThreadLocalWithValidHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/xroad/v1/taxpayer/123");
        request.addHeader(XRoadHeaders.CLIENT, "KH/GOV/MOF/BUDGET-SYSTEM");
        request.addHeader(XRoadHeaders.ID, "msg-001");
        request.addHeader(XRoadHeaders.USER_ID, "EE12345678901");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Use a holder to capture context inside filter chain
        final XRoadContextDto[] captured = new XRoadContextDto[1];
        MockFilterChain capturingChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                captured[0] = XRoadRequestFilter.getContext();
            }
        };

        filter.doFilterInternal(request, response, capturingChain);

        assertThat(captured[0]).isNotNull();
        assertThat(captured[0].getClientInstance()).isEqualTo("KH");
        assertThat(captured[0].getClientMemberClass()).isEqualTo("GOV");
        assertThat(captured[0].getClientMemberCode()).isEqualTo("MOF");
        assertThat(captured[0].getClientSubsystem()).isEqualTo("BUDGET-SYSTEM");
        assertThat(captured[0].getMessageId()).isEqualTo("msg-001");
        assertThat(captured[0].getUserId()).isEqualTo("EE12345678901");
        assertThat(captured[0].isGovernmentRequest()).isTrue();

        // ThreadLocal should be cleaned up after filter
        assertThat(XRoadRequestFilter.getContext()).isNull();
    }

    @Test
    @DisplayName("Should return 400 when X-Road-Client header is missing")
    void shouldReturn400WhenClientHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/xroad/v1/service");
        request.addHeader(XRoadHeaders.ID, "msg-002");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should return 400 when X-Road-Id header is missing")
    void shouldReturn400WhenIdHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/xroad/v1/service");
        request.addHeader(XRoadHeaders.CLIENT, "KH/GOV/MOF/BUDGET-SYSTEM");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should return 400 when X-Road-Client header has invalid format")
    void shouldReturn400WhenClientHeaderInvalidFormat() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/xroad/v1/service");
        request.addHeader(XRoadHeaders.CLIENT, "INVALID-FORMAT");
        request.addHeader(XRoadHeaders.ID, "msg-003");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should not filter non-xroad requests")
    void shouldNotFilterNonXroadRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");

        boolean shouldNotFilter = filter.shouldNotFilter(request);

        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("Should return 503 when X-Road is disabled")
    void shouldReturn503WhenXroadDisabled() throws Exception {
        ReflectionTestUtils.setField(filter, "xroadEnabled", false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/xroad/v1/service");
        request.addHeader(XRoadHeaders.CLIENT, "KH/GOV/MOF/BUDGET-SYSTEM");
        request.addHeader(XRoadHeaders.ID, "msg-004");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
    }

    @Test
    @DisplayName("Should clean up ThreadLocal after request completes")
    void shouldCleanUpThreadLocal() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/xroad/v1/service");
        request.addHeader(XRoadHeaders.CLIENT, "KH/COM/ABA/BANK-SYSTEM");
        request.addHeader(XRoadHeaders.ID, "msg-005");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(XRoadRequestFilter.getContext()).isNull();
        assertThat(XRoadRequestFilter.isXRoadRequest()).isFalse();
    }
}
