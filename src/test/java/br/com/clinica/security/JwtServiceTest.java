package br.com.clinica.security;

import br.com.clinica.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.core.GrantedAuthority;

class JwtServiceTest {

    @Test
    void rejectsSignatureFromAnotherKey() {
        var config = new JwtConfig();
        var encoder = config.jwtEncoder(JwtTestSupport.randomKey());
        var decoder = config.jwtDecoder(JwtTestSupport.randomKey(), "clinica-api-test");
        Instant now = Instant.now();
        String token = JwtTestSupport.token(encoder, "clinica-api-test", List.of("ROLE_ADMIN"),
                now, now.plusSeconds(900), now);
        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsExpiredTokenWithoutWaiting() {
        var config = new JwtConfig();
        var key = JwtTestSupport.randomKey();
        Instant now = Instant.now();
        String token = JwtTestSupport.token(config.jwtEncoder(key), "clinica-api-test", List.of(),
                now.minusSeconds(1800), now.minusSeconds(120), now.minusSeconds(1800));
        assertThrows(JwtException.class,
                () -> config.jwtDecoder(key, "clinica-api-test").decode(token));
    }

    @Test
    void rejectsTokenNotYetValid() {
        var config = new JwtConfig();
        var key = JwtTestSupport.randomKey();
        Instant now = Instant.now();
        String token = JwtTestSupport.token(config.jwtEncoder(key), "clinica-api-test", List.of(),
                now, now.plusSeconds(900), now.plusSeconds(300));
        assertThrows(JwtException.class,
                () -> config.jwtDecoder(key, "clinica-api-test").decode(token));
    }

    @Test
    void rejectsUnexpectedIssuer() {
        var config = new JwtConfig();
        var key = JwtTestSupport.randomKey();
        Instant now = Instant.now();
        String token = JwtTestSupport.token(config.jwtEncoder(key), "outro-emissor", List.of(),
                now, now.plusSeconds(900), now);
        assertThrows(JwtException.class,
                () -> config.jwtDecoder(key, "clinica-api-test").decode(token));
    }

    @Test
    void rejectsPayloadChangedAfterSigning() {
        var config = new JwtConfig();
        var key = JwtTestSupport.randomKey();
        Instant now = Instant.now();
        String token = JwtTestSupport.token(config.jwtEncoder(key), "clinica-api-test",
                List.of("ROLE_RECEPTIONIST"), now, now.plusSeconds(900), now);
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]),
                java.nio.charset.StandardCharsets.UTF_8).replace("ROLE_RECEPTIONIST", "ROLE_ADMIN");
        String changed = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                + "." + parts[2];
        assertThrows(JwtException.class,
                () -> config.jwtDecoder(key, "clinica-api-test").decode(changed));
    }

    @Test
    void convertsAuthoritiesWithoutAddingAnotherPrefix() {
        var config = new JwtConfig();
        var key = JwtTestSupport.randomKey();
        Instant now = Instant.now();
        String token = JwtTestSupport.token(config.jwtEncoder(key), "clinica-api-test",
                List.of("ROLE_PROFESSIONAL"), now, now.plusSeconds(900), now);
        var jwt = config.jwtDecoder(key, "clinica-api-test").decode(token);
        var authentication = config.jwtAuthenticationConverter().convert(jwt);
        assertNotNull(authentication);
        assertEquals("usuario@example.com", authentication.getName());
        assertEquals(List.of("ROLE_PROFESSIONAL"), authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_")).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"token-invalido", "a.b.c"})
    void rejectsMalformedToken(String token) {
        var decoder = new JwtConfig().jwtDecoder(JwtTestSupport.randomKey(), "clinica-api-test");
        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsShortOrInvalidBase64Secret() {
        var config = new JwtConfig();
        assertThrows(IllegalArgumentException.class,
                () -> config.jwtSecretKey(Base64.getEncoder().encodeToString(new byte[31])));
        assertThrows(IllegalArgumentException.class, () -> config.jwtSecretKey("!invalid-base64!"));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveExpiration(long seconds) {
        var encoder = new JwtConfig().jwtEncoder(JwtTestSupport.randomKey());
        assertThrows(IllegalArgumentException.class,
                () -> new JwtService(encoder, "clinica-api-test", seconds));
    }

    @Test
    void generatesValidSignedTokenWithExpectedClaims() {
        // Chave exclusiva do teste: não usa configurações locais.
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);

        var config = new JwtConfig();
        var key = config.jwtSecretKey(Base64.getEncoder().encodeToString(keyBytes));
        String issuer = "clinica-api-test";
        long expirationSeconds = 900;

        var encoder = config.jwtEncoder(key);
        var decoder = config.jwtDecoder(key, issuer);
        var service = new JwtService(encoder, issuer, expirationSeconds);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var response = service.generateToken(authentication);
        Instant after = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // O decoder real verifica a assinatura e as validações configuradas.
        var jwt = decoder.decode(response.accessToken());

        assertEquals("Bearer", response.tokenType());
        assertEquals(expirationSeconds, response.expiresIn());
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertEquals("JWT", jwt.getHeaders().get("typ"));
        assertEquals(issuer, jwt.getClaimAsString("iss"));
        assertEquals(authentication.getName(), jwt.getSubject());
        assertEquals(List.of("ROLE_ADMIN"), jwt.getClaimAsStringList("authorities"));

        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());
        assertFalse(jwt.getIssuedAt().isBefore(before));
        assertFalse(jwt.getIssuedAt().isAfter(after));
        assertEquals(jwt.getIssuedAt().plusSeconds(expirationSeconds), jwt.getExpiresAt());
        assertFalse(jwt.getClaims().containsKey("password"));
    }
}
