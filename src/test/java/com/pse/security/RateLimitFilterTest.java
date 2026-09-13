package com.pse.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private MockHttpServletRequest apiRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/search");
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    void allowsUpToLimitThenReturns429() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 3);

        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(apiRequest("10.0.0.1"), response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(apiRequest("10.0.0.1"), blocked, new MockFilterChain());

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
        assertThat(blocked.getContentAsString()).contains("Rate limit exceeded");
    }

    @Test
    void limitIsPerClient() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 1);

        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(apiRequest("1.1.1.1"), first, new MockFilterChain());
        assertThat(first.getStatus()).isEqualTo(HttpServletResponse.SC_OK);

        MockHttpServletResponse other = new MockHttpServletResponse();
        filter.doFilter(apiRequest("2.2.2.2"), other, new MockFilterChain());
        assertThat(other.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void nonApiPathsAndDisabledFilterAreNotLimited() throws Exception {
        RateLimitFilter disabled = new RateLimitFilter(false, 1);
        MockHttpServletResponse r1 = new MockHttpServletResponse();
        disabled.doFilter(apiRequest("9.9.9.9"), r1, new MockFilterChain());
        disabled.doFilter(apiRequest("9.9.9.9"), new MockHttpServletResponse(), new MockFilterChain());
        assertThat(r1.getStatus()).isEqualTo(HttpServletResponse.SC_OK);

        RateLimitFilter enabled = new RateLimitFilter(true, 1);
        MockHttpServletRequest actuator = new MockHttpServletRequest("GET", "/actuator/health");
        actuator.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse r2 = new MockHttpServletResponse();
        enabled.doFilter(actuator, r2, new MockFilterChain());
        enabled.doFilter(actuator, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(r2.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }
}
