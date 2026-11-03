package com.sky.utils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
public final class JwtUtil {
    private JwtUtil() {}
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        return Jwts.builder().setClaims(new HashMap<>(claims)).setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes(StandardCharsets.UTF_8)).compact();
    }
    public static Claims parseJWT(String secretKey, String token) {
        return Jwts.parser().setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token).getBody();
    }
}
