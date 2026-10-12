package it.giovannidefilippo.gestionale.common;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void propagatesValidRequestIdAndCleansRequestContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader(CorrelationId.HEADER_NAME, "request-safe-001");
        request.setQueryString("password=never-log-this");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("unrelated", "preserved");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationId.HEADER_NAME)).isEqualTo("request-safe-001");
        assertThat(request.getAttribute(CorrelationId.ATTRIBUTE_NAME)).isEqualTo("request-safe-001");
        assertThat(MDC.get(CorrelationId.ATTRIBUTE_NAME)).isNull();
        assertThat(MDC.get("http_method")).isNull();
        assertThat(MDC.get("http_path")).isNull();
        assertThat(MDC.get("http_status")).isNull();
        assertThat(MDC.get("duration_ms")).isNull();
        assertThat(MDC.get("unrelated")).isEqualTo("preserved");
        MDC.clear();
    }
}
