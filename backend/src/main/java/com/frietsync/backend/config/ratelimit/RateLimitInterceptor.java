package com.frietsync.backend.config.ratelimit;

import com.frietsync.backend.service.impl.ratelimit.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (request.getMethod().equals("OPTIONS")) {
            return true;
        }

        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();

        int limit = 10;

        if (path.equals("/api/v1/auth/signup") || path.equals("/api/v1/auth/forgot-password")) {
            limit = 10;
        } else if (path.equals("/api/v1/auth/login")) {
            limit = 10;
        } else if (path.equals("/api/v1/auth/refresh")) {
            limit = 15;
        } else if (path.equals("/api/v1/auth/logout")) {
            limit = 10;
        }

        String key = "rate:" + ip + ":" + path;
        boolean allowed = rateLimitService.isAllowed(key, limit, Duration.ofMinutes(1));

        if (!allowed) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"message\":\"Too many requests, try again in a minute\"}");
            return false;
        }

        return true;
    }
}
