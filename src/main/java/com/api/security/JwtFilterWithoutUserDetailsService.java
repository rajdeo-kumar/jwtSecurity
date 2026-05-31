package com.api.security;

import com.api.messages.AppMessages;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Filter for authenticating requests based on JWT tokens.
 * This filter intercepts incoming HTTP requests, extracts the JWT token from the Authorization header,
 * validates it, and sets the authentication in the SecurityContext if valid. (without UserDetailsService)
 */
@Component
public class JwtFilterWithoutUserDetailsService extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(JwtFilterWithoutUserDetailsService.class);

	private static final String BEARER_PREFIX =
			"Bearer ";

	@Autowired
	private JwtUtils jwtUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request,	HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
		try {
			/*
			 * Skip if already authenticated
			 */
			if (SecurityContextHolder.getContext().getAuthentication() != null) {
				chain.doFilter(request, response);
				return;
			}

			/*
			 * ==========================================
			 * JWT AUTHENTICATION
			 * ==========================================
			 */
			String jwt = extractJwtToken(request);
			/*
			 * No JWT present
			 */
			if (jwt == null) {
				chain.doFilter(request, response);
				return;
			}
			logger.info("JWT Token Received");
			authenticateJwt(jwt, request, response);
			/*
			 * Continue only if response not committed
			 */
			if (!response.isCommitted()) {
				chain.doFilter(request, response);
			}

		} catch (Exception ex) {
			SecurityContextHolder.clearContext();
			logger.error("JWT authentication failed", ex);
			sendUnauthorizedResponse(response, request,	AppMessages.AUTHENTICATION_FAILED
			);
		}
	}

	/**
	 * ==========================================
	 * Extract JWT token
	 * ==========================================
	 */
	private String extractJwtToken(HttpServletRequest request) {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorizationHeader == null	|| !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}

	/**
	 * ==========================================
	 * JWT AUTHENTICATION
	 * ==========================================
	 */
	private void authenticateJwt(String jwt, HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			/*
			 * Validate JWT
			 */
			if (!jwtUtil.validateJwtToken(jwt)) {
				sendUnauthorizedResponse(response, request, AppMessages.INVALID_JWT);
				return;
			}

			/*
			 * Extract subject
			 */
			String subject = jwtUtil.getUsernameFromJwtToken(jwt);
			if (subject == null || subject.trim().isEmpty()) {
				sendUnauthorizedResponse(response, request, AppMessages.INVALID_JWT);
				return;
			}

			/*
			 * DIRECT AUTHENTICATION
			 * NO DB LOOKUP
			 */
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			logger.info("JWT authentication successful for subject={}",	subject);
		} catch (ExpiredJwtException ex) {
			logger.error("JWT token expired", ex);
			sendUnauthorizedResponse(response, request, AppMessages.JWT_EXPIRED);
		} catch (MalformedJwtException ex) {
			logger.error("Invalid JWT token", ex);
			sendUnauthorizedResponse(response, request, AppMessages.INVALID_JWT);
		} catch (Exception ex) {
			logger.error("Unable to parse JWT token",ex);
			sendUnauthorizedResponse(response, request,	AppMessages.AUTHENTICATION_FAILED);
		}
	}

	/**
	 * ==========================================
	 * Unauthorized Response
	 * ==========================================
	 */
	private void sendUnauthorizedResponse(
			HttpServletResponse response,
			HttpServletRequest request,
			String message)
			throws IOException {

		if (response.isCommitted()) {
			return;
		}

		response.setStatus(
				HttpServletResponse.SC_UNAUTHORIZED
		);

		response.setContentType(
				MediaType.APPLICATION_JSON_VALUE
		);

		response.getWriter().write(
				String.format("""
                        {
                            "status": 401,
                            "error": "Unauthorized",
                            "message": "%s",
                            "path": "%s"
                        }
                        """,
						message,
						request.getRequestURI()
				)
		);
	}
}


