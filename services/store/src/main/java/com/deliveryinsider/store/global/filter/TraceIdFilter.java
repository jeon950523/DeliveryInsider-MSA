package com.deliveryinsider.store.global.filter;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER="X-Trace-Id";
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String traceId=req.getHeader(TRACE_HEADER); if(traceId==null||traceId.isBlank()) traceId=UUID.randomUUID().toString();
        MDC.put("traceId",traceId); res.setHeader(TRACE_HEADER,traceId);
        try { chain.doFilter(req,res); } finally { MDC.remove("traceId"); }
    }
}
