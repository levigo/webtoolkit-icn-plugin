package org.jadice.web.icn.plugin;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Builds the signed token that opens a P8 document in jadice web toolkit without uploading it
 * ({@code icn://<token>}). Format: {@code base64url(payload) + "." + base64url(HMAC-SHA256(secret, payload))}
 * without padding; the payload is the canonical JSON
 * {@code {"v":1,"os":"<objectStoreId>","doc":"<documentId>","w":<true|false>,"exp":<unix seconds>,"sub":"<user>"}}
 * ({@code sub} only when a user id is given).
 * The byte sequence must match what {@code IcnTokenCodec} in webtoolkit-content-navigator-integration verifies.
 */
final class IcnToken {

    private static final String HMAC = "HmacSHA256";

    private IcnToken() {
    }

    /**
     * @param secret        the shared secret configured in the plugin and in jadice web toolkit
     * @param objectStoreId the P8 object store id
     * @param documentId    the P8 document id
     * @param writable      whether the ICN user may view and edit annotations
     * @param expiresAt     expiry as Unix seconds
     * @return the token
     */
    static String create(String secret, String objectStoreId, String documentId, boolean writable, long expiresAt) {
        return create(secret, objectStoreId, documentId, writable, expiresAt, null);
    }

    /**
     * @param secret        the shared secret configured in the plugin and in jadice web toolkit
     * @param objectStoreId the P8 object store id
     * @param documentId    the P8 document id
     * @param writable      whether the ICN user may view and edit annotations
     * @param expiresAt     expiry as Unix seconds
     * @param userId        the ICN user id, written as {@code sub} for logging on the jadice side; blank omits it
     * @return the token
     */
    static String create(String secret, String objectStoreId, String documentId, boolean writable, long expiresAt,
                         String userId) {
        String sub = userId == null || userId.trim().isEmpty() ? "" : ",\"sub\":\"" + escape(userId.trim()) + "\"";
        String payload = "{\"v\":1,\"os\":\"" + escape(objectStoreId) + "\",\"doc\":\"" + escape(documentId)
                + "\",\"w\":" + writable + ",\"exp\":" + expiresAt + sub + "}";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC));
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return encoder.encodeToString(payloadBytes) + "." + encoder.encodeToString(mac.doFinal(payloadBytes));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(HMAC + " not available", e);
        }
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
