package com.br.elohostel.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class TenantContext {
    
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static String getCurrentTenant() {
        String tenant = currentTenant.get();
        if (tenant != null) {
            return tenant;
        }
        
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            return jwt.getClaimAsString("tenant");
        }
        
        throw new RuntimeException("Tenant não encontrado no contexto");
    }

    public static void setCurrentTenant(String tenantKey) {
        currentTenant.set(tenantKey);
    }

    public static void clear() {
        currentTenant.remove();
    }
}