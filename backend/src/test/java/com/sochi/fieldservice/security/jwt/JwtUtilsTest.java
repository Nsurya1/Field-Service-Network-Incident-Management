package com.sochi.fieldservice.security.jwt;

import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String secret = "9a3f2c4e6b8a1d5e7f0b2d4c6e8a1f3b5c7d9e1a3b5c7d9e1a3b5c7d9e1a3b5c";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000L); // 1 hour
    }

    @Test
    void shouldGenerateAndValidateTokenSuccessfully() {
        User user = new User("admin_user", "admin@example.com", "pass", Role.ROLE_ADMIN);

        String token = jwtUtils.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals("admin_user", jwtUtils.extractUsername(token));
        assertEquals("ROLE_ADMIN", jwtUtils.extractRole(token));
        assertNotNull(jwtUtils.extractExpiration(token));
        assertTrue(jwtUtils.extractExpiration(token).after(new Date()));
        assertFalse(jwtUtils.isTokenExpired(token));
        assertTrue(jwtUtils.validateToken(token, user));
    }

    @Test
    void shouldRejectTokenWhenUsernameDoesNotMatch() {
        User user1 = new User("alice", "alice@example.com", "pass", Role.ROLE_ENGINEER);
        User user2 = new User("bob", "bob@example.com", "pass", Role.ROLE_ENGINEER);

        String token = jwtUtils.generateToken(user1);

        assertFalse(jwtUtils.validateToken(token, user2));
    }

    @Test
    void shouldHandleExpiredToken() {
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1000L); // expired 1 sec ago
        User user = new User("charlie", "charlie@example.com", "pass", Role.ROLE_MANAGER);

        String expiredToken = jwtUtils.generateToken(user);

        assertTrue(jwtUtils.isTokenExpired(expiredToken));
        assertFalse(jwtUtils.validateToken(expiredToken, user));
    }

    @Test
    void shouldHandleMalformedTokenGracefully() {
        String invalidToken = "not.a.valid.jwt.token";

        assertTrue(jwtUtils.isTokenExpired(invalidToken));
        User user = new User("test", "test@example.com", "pass", Role.ROLE_ENGINEER);
        assertFalse(jwtUtils.validateToken(invalidToken, user));
    }

    @Test
    void shouldThrowWhenSigningKeyRequestedWithoutSecret() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "");
        User user = new User("admin_user", "admin@example.com", "pass", Role.ROLE_ADMIN);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> jwtUtils.generateToken(user)
        );
        assertTrue(ex.getMessage().contains("JWT secret"));
    }

    @Test
    void shouldFailValidationInProdWhenSecretIsEmpty() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        JwtUtils utils = new JwtUtils("", 3600000L, env);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                utils::validateConfiguration
        );
        assertTrue(ex.getMessage().contains("must not be empty in production profile"));
    }

    @Test
    void shouldFailValidationInProdWhenSecretIsShorterThan32Bytes() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        JwtUtils utils = new JwtUtils("short-secret-key-12345", 3600000L, env);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                utils::validateConfiguration
        );
        assertTrue(ex.getMessage().contains("must be at least 32 bytes"));
    }

    @Test
    void shouldPassValidationInProdWhenSecretIsAtLeast32Bytes() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        JwtUtils utils = new JwtUtils("this-is-a-valid-production-secret-key-32bytes", 3600000L, env);

        assertDoesNotThrow(utils::validateConfiguration);
    }

    @Test
    void shouldFailValidationInProdWhenConfiguredViaDefaultProfiles() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{});
        when(env.getDefaultProfiles()).thenReturn(new String[]{"prod"});

        JwtUtils utils = new JwtUtils(null, 3600000L, env);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                utils::validateConfiguration
        );
        assertTrue(ex.getMessage().contains("must not be empty in production profile"));
    }

    @Test
    void shouldNotFailValidationInDevProfileEvenIfSecretIsEmpty() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev"});

        JwtUtils utils = new JwtUtils("", 3600000L, env);

        assertDoesNotThrow(utils::validateConfiguration);
    }
}
