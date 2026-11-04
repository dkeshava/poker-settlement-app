package com.dk.pokersettlement.config;

import com.dk.pokersettlement.services.JWTService;
import com.dk.pokersettlement.services.MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private JWTService jwtService;
    @Autowired
    private ApplicationContext applicationContext;
    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        String authHeader = request.getHeader("Authorization");
//        String token = authHeader == null ? null : authHeader.replace("Bearer ", "");
//        String username = null;
//        if (token != null) {
//            username=jwtService.extractUserName(token);
//        }
//        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//            UserDetails userDetails=applicationContext.getBean(MyUserDetailsService.class).loadUserByUsername(username);
//            if(jwtService.validateToken(token,userDetails)){
//                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                SecurityContextHolder.getContext().setAuthentication(authentication);
//            }
//        }
//        filterChain.doFilter(request,response);
//    }
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : null;

        System.out.println("Request: " + request.getMethod()
                + " " + request.getRequestURI());
        System.out.println("Token present: " + (token != null));

        if (token != null) {
            try {
                String username = jwtService.extractUserName(token);
                System.out.println("Username extracted: " + username);

                if (username != null &&
                        SecurityContextHolder.getContext()
                                .getAuthentication() == null) {

                    UserDetails userDetails = applicationContext
                            .getBean(MyUserDetailsService.class)
                            .loadUserByUsername(username);

                    boolean valid = jwtService.validateToken(token, userDetails);
                    System.out.println("Token valid: " + valid);

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

                        System.out.println("Authentication set successfully");
                    }
                }
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                System.out.println("JWT EXPIRED: " + e.getMessage());
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "JWT expired"
                );
                return;
            } catch (io.jsonwebtoken.JwtException e) {
                System.out.println("JWT INVALID: " + e.getMessage());
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid JWT"
                );
                return;
            }
        }

        System.out.println("Before filterChain.doFilter()");

        filterChain.doFilter(request, response);

        System.out.println("After filterChain.doFilter()");
        System.out.println("Response status: " + response.getStatus());
    }
}
