package pk.wj.pasir_wenek_jakub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pk.wj.pasir_wenek_jakub.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final long EXPIRATION_MS = 3_600_000L; // 1 godzina

    private final SecretKey key;

    public JwtUtil(
            @Value("${JWT_SECRET}") String jwtSecret
    ) {
        // Walidacja: czy sekret został skonfigurowany
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }

        // Walidacja: wymagana długość co najmniej 64 bajtów dla algorytmu HS512
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalStateException("JWT secret must be at least 64 bytes for HS512");
        }

        // Tworzenie klucza kryptograficznego na podstawie ciągu znaków
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }


    /**
     * Generuje nowy token JWT dla użytkownika, dodając jego ID i email do 'claims'.
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("email", user.getEmail());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Parsuje token i zwraca wszystkie zawarte w nim informacje (payload).
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Wyciąga nazwę użytkownika (email) z pola 'subject' tokena.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Sprawdza, czy token jest poprawny i nie wygasł.
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }

    }
    /**
     * Wyciąga adres email użytkownika z pola 'subject' tokena.
     */
    public String extractEmail(String token) {
        return extractUsername(token);
    }

}