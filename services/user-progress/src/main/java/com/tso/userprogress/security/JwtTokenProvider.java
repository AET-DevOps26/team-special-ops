package com.tso.userprogress.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final SecretKey jwtSecret;
  private final long jwtExpiration;
  private final long refreshExpiration;

  public JwtTokenProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration}") long expiration,
      @Value("${jwt.refresh-expiration}") long refreshExpiration) {
    this.jwtSecret = Keys.hmacShaKeyFor(secret.getBytes());
    this.jwtExpiration = expiration;
    this.refreshExpiration = refreshExpiration;
  }

  /**
   * Generate a JWT access token for the given user.
   *
   * @param userId the user ID
   * @param email the user email
   * @return JWT token string
   */
  public String generateAccessToken(UUID userId, String email) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration * 1000);

    return Jwts.builder()
        .setSubject(userId.toString())
        .claim("email", email)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(jwtSecret, SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Generate a JWT refresh token for the given user.
   *
   * @param userId the user ID
   * @return JWT refresh token string
   */
  public String generateRefreshToken(UUID userId) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + refreshExpiration * 1000);

    return Jwts.builder()
        .setSubject(userId.toString())
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(jwtSecret, SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Extract user ID from JWT token.
   *
   * @param token the JWT token
   * @return the user ID as UUID
   * @throws JwtException if token is invalid or expired
   */
  public UUID getUserIdFromToken(String token) {
    Claims claims = getClaims(token);
    return UUID.fromString(claims.getSubject());
  }

  /**
   * Extract email from JWT token.
   *
   * @param token the JWT token
   * @return the email
   * @throws JwtException if token is invalid or expired
   */
  public String getEmailFromToken(String token) {
    Claims claims = getClaims(token);
    return claims.get("email", String.class);
  }

  /**
   * Validate JWT token signature and expiration.
   *
   * @param token the JWT token
   * @return true if valid, false otherwise
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
      return true;
    } catch (MalformedJwtException e) {
      // Invalid JWT format
      return false;
    } catch (ExpiredJwtException e) {
      // Token expired
      return false;
    } catch (UnsupportedJwtException e) {
      // Unsupported JWT
      return false;
    } catch (IllegalArgumentException e) {
      // JWT is empty
      return false;
    } catch (SignatureException e) {
      // Signature validation failed
      return false;
    }
  }

  /**
   * Get all claims from the JWT token.
   *
   * @param token the JWT token
   * @return Claims object
   * @throws JwtException if token is invalid or expired
   */
  private Claims getClaims(String token) {
    return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
  }
}
