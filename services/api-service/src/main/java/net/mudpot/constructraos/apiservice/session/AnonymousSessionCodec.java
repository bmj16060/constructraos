package net.mudpot.constructraos.apiservice.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AnonymousSessionCodec {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AnonymousSessionCodec() {
    }

    public static String sign(final Map<String, Object> payload, final String secret) {
        final String normalizedSecret = normalizeSecret(secret);
        final Map<String, Object> signedPayload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        signedPayload.putIfAbsent("issued_at", Instant.now().toString());
        try {
            final String payloadJson = OBJECT_MAPPER.writeValueAsString(signedPayload);
            final String payloadToken = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
            final byte[] signature = hmacSha256(payloadToken, normalizedSecret);
            return payloadToken + "." + base64Url(signature);
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed signing anonymous session.", exception);
        }
    }

    public static Map<String, Object> verifyAndParse(final String token, final String secret) {
        final String normalizedSecret = normalizeSecret(secret);
        final String raw = token == null ? "" : token.trim();
        final String[] parts = raw.split("\\.");
        if (parts.length != 2) {
            return Map.of();
        }
        try {
            final String payloadPart = parts[0];
            final byte[] signature = Base64.getUrlDecoder().decode(parts[1]);
            final byte[] expected = hmacSha256(payloadPart, normalizedSecret);
            if (!MessageDigest.isEqual(signature, expected)) {
                return Map.of();
            }
            final byte[] payload = Base64.getUrlDecoder().decode(payloadPart);
            return OBJECT_MAPPER.readValue(payload, new TypeReference<>() { });
        } catch (final Exception ignored) {
            return Map.of();
        }
    }

    private static String normalizeSecret(final String secret) {
        final String normalizedSecret = secret == null ? "" : secret.trim();
        if (normalizedSecret.isBlank()) {
            throw new IllegalStateException("ANON_SESSION_SIGNING_SECRET is required.");
        }
        return normalizedSecret;
    }

    private static byte[] hmacSha256(final String payloadPart, final String secret) throws Exception {
        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(payloadPart.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64Url(final byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
