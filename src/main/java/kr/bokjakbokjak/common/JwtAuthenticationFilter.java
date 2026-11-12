package kr.bokjakbokjak.common;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import kr.bokjakbokjak.auth.token.AuthTokenStore;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

final class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AuthTokenStore tokenStore;
    private final AuthenticationEntryPoint entryPoint;

    JwtAuthenticationFilter(JwtService jwtService, AuthTokenStore tokenStore, AuthenticationEntryPoint entryPoint) {
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (!header.regionMatches(true, 0, "Bearer ", 0, 7) || header.length() == 7) {
                throw new IllegalArgumentException("Invalid Authorization header");
            }
            JwtService.AccessClaims claims = jwtService.readAccessToken(header.substring(7));
            if (claims.tokenId() != null && tokenStore.isAccessTokenBlocked(claims.tokenId())) {
                throw new IllegalArgumentException("Logged out access token");
            }
            var authentication = UsernamePasswordAuthenticationToken.authenticated(claims.userId(), null, List.of());
            // 로그아웃할 때 현재 토큰을 차단하려고 토큰 정보를 함께 둔다
            authentication.setDetails(claims);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response, new BadCredentialsException("Invalid access token", exception));
            return;
        }

        chain.doFilter(request, response);
    }
}
