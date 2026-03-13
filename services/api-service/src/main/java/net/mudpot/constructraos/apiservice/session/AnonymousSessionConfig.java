package net.mudpot.constructraos.apiservice.session;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

@Singleton
public class AnonymousSessionConfig {
    @Value("${anon-session.cookie-name}")
    private String cookieName;

    @Value("${anon-session.signing-secret}")
    private String signingSecret;

    @Value("${anon-session.cookie-secure}")
    private boolean cookieSecure;

    @Value("${anon-session.cookie-domain:}")
    private String cookieDomain;

    @Value("${anon-session.max-age-days}")
    private long maxAgeDays;

    public String cookieName() {
        return cookieName;
    }

    public String signingSecret() {
        return signingSecret;
    }

    public boolean cookieSecure() {
        return cookieSecure;
    }

    public String cookieDomain() {
        return cookieDomain;
    }

    public long maxAgeDays() {
        return maxAgeDays;
    }
}
