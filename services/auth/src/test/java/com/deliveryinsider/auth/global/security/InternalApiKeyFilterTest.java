package com.deliveryinsider.auth.global.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalApiKeyFilterTest {

    private static final String INTERNAL_API_KEY =
        "deliveryinsider-local-internal-key-20260813";

    @Test
    void validInternalApiKeyPasses() throws Exception {
        InternalApiKeyFilter filter =
            new InternalApiKeyFilter(INTERNAL_API_KEY);

        MockHttpServletRequest request =
            new MockHttpServletRequest(
                "GET",
                "/internal/security-check"
            );

        request.addHeader(
            "X-Internal-Api-Key",
            INTERNAL_API_KEY
        );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        AtomicBoolean chainCalled =
            new AtomicBoolean(false);

        FilterChain chain = (req, res) ->
            chainCalled.set(true);

        filter.doFilter(
            request,
            response,
            chain
        );

        assertTrue(chainCalled.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void missingInternalApiKeyReturnsUnauthorized()
        throws Exception {

        InternalApiKeyFilter filter =
            new InternalApiKeyFilter(INTERNAL_API_KEY);

        MockHttpServletRequest request =
            new MockHttpServletRequest(
                "GET",
                "/internal/security-check"
            );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        AtomicBoolean chainCalled =
            new AtomicBoolean(false);

        FilterChain chain = (req, res) ->
            chainCalled.set(true);

        filter.doFilter(
            request,
            response,
            chain
        );

        assertFalse(chainCalled.get());
        assertEquals(401, response.getStatus());
        assertTrue(
            response.getContentAsString()
                .contains("COMMON-401-001")
        );
    }

    @Test
    void wrongInternalApiKeyReturnsUnauthorized()
        throws Exception {

        InternalApiKeyFilter filter =
            new InternalApiKeyFilter(INTERNAL_API_KEY);

        MockHttpServletRequest request =
            new MockHttpServletRequest(
                "GET",
                "/internal/security-check"
            );

        request.addHeader(
            "X-Internal-Api-Key",
            "wrong-key"
        );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        AtomicBoolean chainCalled =
            new AtomicBoolean(false);

        FilterChain chain = (req, res) ->
            chainCalled.set(true);

        filter.doFilter(
            request,
            response,
            chain
        );

        assertFalse(chainCalled.get());
        assertEquals(401, response.getStatus());
    }

    @Test
    void missingServerConfigurationReturnsServiceUnavailable()
        throws Exception {

        InternalApiKeyFilter filter =
            new InternalApiKeyFilter("");

        MockHttpServletRequest request =
            new MockHttpServletRequest(
                "GET",
                "/internal/security-check"
            );

        request.addHeader(
            "X-Internal-Api-Key",
            INTERNAL_API_KEY
        );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        AtomicBoolean chainCalled =
            new AtomicBoolean(false);

        FilterChain chain = (req, res) ->
            chainCalled.set(true);

        filter.doFilter(
            request,
            response,
            chain
        );

        assertFalse(chainCalled.get());
        assertEquals(503, response.getStatus());
        assertTrue(
            response.getContentAsString()
                .contains("COMMON-503-001")
        );
    }

    @Test
    void publicApiDoesNotRequireInternalApiKey()
        throws Exception {

        InternalApiKeyFilter filter =
            new InternalApiKeyFilter(INTERNAL_API_KEY);

        MockHttpServletRequest request =
            new MockHttpServletRequest(
                "GET",
                "/api/auth/me"
            );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        AtomicBoolean chainCalled =
            new AtomicBoolean(false);

        FilterChain chain = (req, res) ->
            chainCalled.set(true);

        filter.doFilter(
            request,
            response,
            chain
        );

        assertTrue(chainCalled.get());
    }
}
