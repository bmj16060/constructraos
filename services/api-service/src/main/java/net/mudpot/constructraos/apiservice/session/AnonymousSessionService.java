package net.mudpot.constructraos.apiservice.session;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.cookie.SameSite;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class AnonymousSessionService {
    private static final String DEFAULT_ACTOR_KIND = "anonymous";

    private final AnonymousSessionConfig config;

    public AnonymousSessionService(final AnonymousSessionConfig config) {
        this.config = config;
    }

    public AnonymousSession ensureSession(final HttpRequest<?> request) {
        final String rawToken = request.getCookies().findCookie(config.cookieName())
            .map(Cookie::getValue)
            .orElse("");
        final Map<String, Object> payload = AnonymousSessionCodec.verifyAndParse(rawToken, config.signingSecret());
        final String sessionId = stringValue(payload.get("session_id"));
        final Instant issuedAt = parseInstant(payload.get("issued_at"));
        final String actorKind = stringValue(payload.get("actor_kind"));
        if (!sessionId.isBlank() && issuedAt != null) {
            return new AnonymousSession(sessionId, actorKind.isBlank() ? DEFAULT_ACTOR_KIND : actorKind, issuedAt, false);
        }
        return new AnonymousSession(UUID.randomUUID().toString(), DEFAULT_ACTOR_KIND, Instant.now(), true);
    }

    public <T> MutableHttpResponse<T> attachCookieIfNeeded(final MutableHttpResponse<T> response, final AnonymousSession session) {
        if (!session.fresh()) {
            return response;
        }
        response.cookie(sessionCookie(session));
        return response;
    }

    public AnonymousSessionResponse toResponse(final AnonymousSession session) {
        return new AnonymousSessionResponse(session.sessionId(), session.actorKind(), session.issuedAt().toString());
    }

    private Cookie sessionCookie(final AnonymousSession session) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("session_id", session.sessionId());
        payload.put("actor_kind", session.actorKind());
        payload.put("issued_at", session.issuedAt().toString());
        final Cookie cookie = Cookie.of(config.cookieName(), AnonymousSessionCodec.sign(payload, config.signingSecret()))
            .httpOnly(true)
            .path("/")
            .sameSite(SameSite.Lax)
            .secure(config.cookieSecure())
            .maxAge(Duration.ofDays(config.maxAgeDays()));
        if (!config.cookieDomain().isBlank()) {
            cookie.domain(config.cookieDomain());
        }
        return cookie;
    }

    private static String stringValue(final Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static Instant parseInstant(final Object value) {
        final String text = stringValue(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (final Exception ignored) {
            return null;
        }
    }
}
