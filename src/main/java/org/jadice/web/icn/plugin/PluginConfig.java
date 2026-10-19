package org.jadice.web.icn.plugin;

import java.util.Map;

/**
 * The plugin configuration as saved by the admin panel ({@code JwtPluginDojo.ConfigurationPane}), with the
 * validation both sides agree on. Two modes:
 * <ul>
 * <li><b>P8 annotations enabled</b>: P8 documents are opened with a signed token ({@code icn://}), which needs
 * the token secret; nothing is transferred.</li>
 * <li><b>P8 annotations disabled</b>: every document is downloaded from Content Navigator (needs its server URL)
 * and uploaded to the viewer ({@code upload://}); no annotations.</li>
 * </ul>
 * The viewer URL is always required and must be HTTPS: Content Navigator's Content Security Policy
 * ({@code frame-src 'self' https:}) blocks an HTTP viewer in the plugin's iframe.
 */
final class PluginConfig {

    /** Default lifetime of a token (first open) when the admin panel leaves the field empty. */
    static final long DEFAULT_TOKEN_TTL_MINUTES = 480L;
    static final int MIN_SECRET_LENGTH = 32;

    private final String viewerUrl;
    private final String icnServerUrl;
    private final boolean p8AnnotationsEnabled;
    private final String tokenSecret;
    private final long tokenTtlMinutes;

    private PluginConfig(String viewerUrl, String icnServerUrl, boolean p8AnnotationsEnabled, String tokenSecret,
                         long tokenTtlMinutes) {
        this.viewerUrl = viewerUrl;
        this.icnServerUrl = icnServerUrl;
        this.p8AnnotationsEnabled = p8AnnotationsEnabled;
        this.tokenSecret = tokenSecret;
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    /**
     * @param json the parsed configuration ({@code com.ibm.json.java.JSONObject} is a {@code Map})
     */
    static PluginConfig of(Map<?, ?> json) {
        return new PluginConfig(trimmed(json.get("viewerUrl")), trimmed(json.get("icnServerURI")),
                Boolean.TRUE.equals(json.get("p8AnnotationsEnabled")), trimmed(json.get("tokenSecret")),
                parseTtl(json.get("tokenTtlMinutes")));
    }

    /** @return {@code null} when the configuration is usable, otherwise the message to show */
    String validate() {
        if (viewerUrl == null) {
            return "jadice web viewer URL not configured";
        }
        if (!viewerUrl.toLowerCase().startsWith("https://")) {
            return "jadice web viewer URL must use https://";
        }
        if (p8AnnotationsEnabled) {
            if (tokenSecret == null || tokenSecret.length() < MIN_SECRET_LENGTH) {
                return "Token secret (at least " + MIN_SECRET_LENGTH + " characters) is required when P8 annotations "
                        + "are enabled";
            }
        } else if (icnServerUrl == null) {
            return "Content Navigator server URL is required when P8 annotations are disabled";
        }
        return null;
    }

    /**
     * @param serverType ICN's {@code serverType} request parameter ({@code p8}, {@code cm}, ...)
     * @return whether the document is opened with a signed token: P8 annotations enabled and a P8 repository
     */
    boolean usesToken(String serverType) {
        return p8AnnotationsEnabled && "p8".equalsIgnoreCase(serverType);
    }

    String getViewerUrl() {
        return viewerUrl;
    }

    /** @return the Content Navigator base URL for the document download, {@code null} when not configured */
    String getIcnServerUrl() {
        return icnServerUrl;
    }

    boolean isP8AnnotationsEnabled() {
        return p8AnnotationsEnabled;
    }

    String getTokenSecret() {
        return tokenSecret;
    }

    long getTokenTtlMinutes() {
        return tokenTtlMinutes;
    }

    private static String trimmed(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private static long parseTtl(Object configured) {
        if (configured == null) {
            return DEFAULT_TOKEN_TTL_MINUTES;
        }
        try {
            long ttl = Long.parseLong(String.valueOf(configured).trim());
            return ttl > 0 ? ttl : DEFAULT_TOKEN_TTL_MINUTES;
        } catch (NumberFormatException e) {
            return DEFAULT_TOKEN_TTL_MINUTES;
        }
    }
}
