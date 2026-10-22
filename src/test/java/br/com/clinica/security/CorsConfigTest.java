package br.com.clinica.security;

import br.com.clinica.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {
    @Test void emptyConfigurationDoesNotAllowAnyOrigin() {
        var source = new CorsConfig().corsConfigurationSource("");
        var config = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/patients"));
        assertNotNull(config);
        assertNull(config.checkOrigin("https://frontend.example.test"));
    }
    @ParameterizedTest
    @ValueSource(strings = {"*", "https://*.example.com", "https://example.com/path", "null", "file:///test",
            "https://user@example.com", "https://example.com?x=1", "https://example.com#fragment"})
    void rejectsNonOriginOrWildcardConfiguration(String origin) {
        assertThrows(IllegalArgumentException.class, () -> new CorsConfig().corsConfigurationSource(origin));
    }
}
