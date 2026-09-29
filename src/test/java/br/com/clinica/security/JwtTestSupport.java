package br.com.clinica.security;

import br.com.clinica.config.JwtConfig;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/** Apenas dados de teste; nenhuma chave ou configuração local é utilizada. */
public final class JwtTestSupport {
    private JwtTestSupport() {}

    public static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static SecretKey randomKey() {
        return new JwtConfig().jwtSecretKey(randomSecret());
    }

    public static String token(JwtEncoder encoder, String issuer, List<String> authorities,
                               Instant issuedAt, Instant expiresAt, Instant notBefore) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject("usuario@example.com")
                .issuedAt(issuedAt).expiresAt(expiresAt).notBefore(notBefore)
                .claim("authorities", authorities).build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
    }
}
