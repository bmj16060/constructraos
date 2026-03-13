package net.mudpot.constructraos.codexbridge.codex;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;

interface CodexAppServerSession extends AutoCloseable {
    JsonNode request(String method, Object params, Duration timeout);

    void notify(String method, Object params);

    @Override
    void close();
}
