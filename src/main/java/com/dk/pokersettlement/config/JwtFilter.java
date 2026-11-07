package com.dk.pokersettlement.config;

import com.dk.pokersettlement.services.JWTService;
import com.dk.pokersettlement.services.MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private JWTService jwtService;
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : null;

        if (token != null) {
            try {
                String username = jwtService.extractUserName(token);

                if (username != null &&
                        SecurityContextHolder.getContext()
                                .getAuthentication() == null) {

                    UserDetails userDetails = applicationContext
                            .getBean(MyUserDetailsService.class)
                            .loadUserByUsername(username);

                    boolean valid = jwtService.validateToken(token, userDetails);

                    if (valid) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request)
                        );

                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);

                        log.debug("Authenticated user: {}", username);
                    }
                }
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                log.warn("JWT expired for request {}: {}", request.getRequestURI(), e.getMessage());
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "JWT expired"
                );
                return;
            } catch (io.jsonwebtoken.JwtException e) {
                log.warn("Invalid JWT for request {}: {}", request.getRequestURI(), e.getMessage());
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid JWT"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
