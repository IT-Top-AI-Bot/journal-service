package com.aquadev.ittopai.config.telegram;

import com.aquadev.ittopai.config.security.PublicEndpoints;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class TelegramUserIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Telegram-User-Id";
    public static final String ATTR = "telegramUserId";

    private static final PathMatcher PATH = new AntPathMatcher();
    private static final String TG_API_PATTERN = "/api/v*/telegram/**";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String raw = request.getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing header: " + HEADER);
            return;
        }

        long telegramUserId;
        try {
            telegramUserId = Long.parseLong(raw);
            if (telegramUserId <= 0) throw new NumberFormatException("must be > 0");
        } catch (NumberFormatException _) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid header: " + HEADER);
            return;
        }

        request.setAttribute(ATTR, telegramUserId);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(telegramUserId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        TelegramUserContext.set(telegramUserId);
        MDC.put("tgUserId", String.valueOf(telegramUserId));
        try {
            filterChain.doFilter(request, response);
        } finally {
            TelegramUserContext.clear();
            MDC.remove("tgUserId");
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !PATH.match(TG_API_PATTERN, uri) || PublicEndpoints.isPublicPath(uri);
    }
}
