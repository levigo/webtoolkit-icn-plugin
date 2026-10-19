package org.jadice.web.icn.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PluginConfigTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private static Map<String, Object> tokenConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("viewerUrl", "https://viewer.example.test/");
        map.put("p8AnnotationsEnabled", Boolean.TRUE);
        map.put("tokenSecret", SECRET);
        return map;
    }

    private static Map<String, Object> uploadConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("viewerUrl", "https://viewer.example.test/");
        map.put("p8AnnotationsEnabled", Boolean.FALSE);
        map.put("icnServerURI", "http://icn.example.test:9081");
        return map;
    }

    @Test
    void tokenConfigurationIsValidAndUsesTokensForP8Documents() {
        PluginConfig config = PluginConfig.of(tokenConfig());

        assertNull(config.validate());
        assertTrue(config.usesToken("p8"));
        assertTrue(config.usesToken("P8"));
        assertFalse(config.usesToken("cm"));
        assertEquals(480L, config.getTokenTtlMinutes());
    }

    @Test
    void uploadConfigurationIsValidAndNeverUsesTokens() {
        PluginConfig config = PluginConfig.of(uploadConfig());

        assertNull(config.validate());
        assertFalse(config.usesToken("p8"));
        assertEquals("http://icn.example.test:9081", config.getIcnServerUrl());
    }

    @Test
    void viewerUrlIsRequiredAndMustBeHttps() {
        Map<String, Object> map = tokenConfig();
        map.remove("viewerUrl");
        assertEquals("jadice web viewer URL not configured", PluginConfig.of(map).validate());

        map.put("viewerUrl", "http://viewer.example.test/");
        assertEquals("jadice web viewer URL must use https://", PluginConfig.of(map).validate());

        map.put("viewerUrl", "HTTPS://viewer.example.test/");
        assertNull(PluginConfig.of(map).validate());
    }

    @Test
    void tokenSecretIsRequiredWithP8Annotations() {
        Map<String, Object> map = tokenConfig();
        map.put("tokenSecret", "");
        assertEquals("Token secret (at least 32 characters) is required when P8 annotations are enabled",
                PluginConfig.of(map).validate());

        map.put("tokenSecret", "too-short");
        assertEquals("Token secret (at least 32 characters) is required when P8 annotations are enabled",
                PluginConfig.of(map).validate());
    }

    @Test
    void icnServerUrlIsRequiredWithoutP8Annotations() {
        Map<String, Object> map = uploadConfig();
        map.put("icnServerURI", " ");
        assertEquals("Content Navigator server URL is required when P8 annotations are disabled",
                PluginConfig.of(map).validate());
    }

    @Test
    void ttlIsTrimmedAndFallsBackToTheDefault() {
        Map<String, Object> map = tokenConfig();
        map.put("tokenTtlMinutes", " 15 ");
        PluginConfig config = PluginConfig.of(map);

        assertEquals("https://viewer.example.test/", config.getViewerUrl());
        assertEquals(15L, config.getTokenTtlMinutes());
        assertEquals(480L, PluginConfig.of(withTtl("abc")).getTokenTtlMinutes());
        assertEquals(480L, PluginConfig.of(withTtl("0")).getTokenTtlMinutes());
    }

    private static Map<String, Object> withTtl(String ttl) {
        Map<String, Object> map = tokenConfig();
        map.put("tokenTtlMinutes", ttl);
        return map;
    }
}
