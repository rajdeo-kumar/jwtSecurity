package com.api.exceptions;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.SignatureException;

@Component
public class CustomJwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		String message = "Access denied.";
		Throwable cause = (Throwable) request.getAttribute("exception");
		if (cause instanceof ExpiredJwtException) {
			message = "JWT token has expired.";
		} else if (cause instanceof SignatureException) {
			message = "Invalid JWT signature.";
		} else if (cause instanceof MalformedJwtException) {
			message = "Invalid JWT token.";
		} else if (cause instanceof UnsupportedJwtException) {
			message = "Unsupported JWT token.";
		} else if (cause instanceof IllegalArgumentException) {
			message = "JWT token is missing.";
		}
		String jsonResponse = """
            {
                "status": 403,
                "error": "Forbidden",
                "message": "%s",
                "path": "%s"
            }
            """.formatted(message, request.getRequestURI());
		response.getWriter().write(jsonResponse);
	}
}
