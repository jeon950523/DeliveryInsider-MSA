package com.deliveryinsider.store.global.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {
    private static final String HEADER="X-Internal-Api-Key";
    private final String configuredKey;
    public InternalApiKeyFilter(@Value("${security.internal.api-key:}") String configuredKey) { this.configuredKey=configuredKey; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return !request.getRequestURI().startsWith("/internal/"); }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        if(configuredKey==null||configuredKey.isBlank()) { write(res,503,"COMMON-503-001","내부 서비스 인증 설정이 없습니다."); return; }
        String provided=req.getHeader(HEADER);
        boolean ok=provided!=null && MessageDigest.isEqual(configuredKey.getBytes(StandardCharsets.UTF_8),provided.getBytes(StandardCharsets.UTF_8));
        if(!ok) { write(res,401,"COMMON-401-001","내부 서비스 인증에 실패했습니다."); return; }
        chain.doFilter(req,res);
    }
    private void write(HttpServletResponse res,int status,String code,String message) throws IOException {
        res.setStatus(status); res.setCharacterEncoding(StandardCharsets.UTF_8.name()); res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"code\":\""+code+"\",\"message\":\""+message+"\",\"data\":null}");
    }
}
