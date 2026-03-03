package com.free.easyLearn.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Utility class for managing secure HttpOnly cookies for JWT tokens.
 * 
 * Tokens are stored in HttpOnly cookies instead of being sent in the response body,
 * preventing XSS attacks from stealing tokens via JavaScript.
 */
@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @Value("${jwt.expiration}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;

    @Value("${cookie.secure:false}")
    private boolean secureCookie;

    @Value("${cookie.domain:}")
    private String cookieDomain;

    @Value("${cookie.same-site:Lax}")
    private String sameSite;

    /**
     * Add access token as HttpOnly cookie to the response.
     */
    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = buildCookie(ACCESS_TOKEN_COOKIE, token, accessTokenExpirationMs / 1000);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Add refresh token as HttpOnly cookie to the response.
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = buildCookie(REFRESH_TOKEN_COOKIE, refreshToken, refreshTokenExpirationMs / 1000);
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Clear both token cookies (used on logout).
     */
    public void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = buildCookie(ACCESS_TOKEN_COOKIE, "", 0);
        ResponseCookie refreshCookie = buildCookie(REFRESH_TOKEN_COOKIE, "", 0);
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    /**
     * Extract access token from cookies.
     */
    public String getAccessTokenFromCookies(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    /**
     * Extract refresh token from cookies.
     */
    public String getRefreshTokenFromCookies(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    /**
     * Build a secure HttpOnly cookie.
     */
    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }

    /**
     * Get a cookie value from the request.
     */
    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
