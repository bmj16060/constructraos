package net.mudpot.constructraos.codexbridge.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
class WebSocketCodexAppServerSessionFactory implements CodexAppServerSessionFactory {
    private static final Map<String, Object> DENIED_EXEC_APPROVAL = Map.of("decision", "denied");
    private static final Map<String, Object> DECLINED_FILE_CHANGE = Map.of("decision", "decline");
    private static final Map<String, Object> EMPTY_USER_INPUT = Map.of("answers", Map.of());
    private static final Map<String, Object> UNSUPPORTED_TOOL_RESPONSE = Map.of(
        "success", false,
        "contentItems", java.util.List.of(Map.of("type", "inputText", "text", "ConstructraOS codex-bridge does not execute dynamic tool calls yet."))
    );

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    WebSocketCodexAppServerSessionFactory(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public CodexAppServerSession open(final URI uri, final Duration timeout) {
        try {
            final WebSocketSession session = new WebSocketSession(objectMapper);
            final WebSocket webSocket = httpClient.newWebSocketBuilder()
                .connectTimeout(timeout)
                .buildAsync(uri, session)
                .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            session.attach(webSocket);
            return session;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted connecting to Codex App Server.", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Failed connecting to Codex App Server.", exception);
        }
    }

    private static final class WebSocketSession implements CodexAppServerSession, WebSocket.Listener {
        private final ObjectMapper objectMapper;
        private final AtomicLong nextRequestId = new AtomicLong(1L);
        private final ConcurrentHashMap<String, CompletableFuture<JsonNode>> pendingResponses = new ConcurrentHashMap<>();
        private final CompletableFuture<Void> closed = new CompletableFuture<>();
        private final StringBuilder currentMessage = new StringBuilder();
        private volatile WebSocket webSocket;

        private WebSocketSession(final ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        private void attach(final WebSocket webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public JsonNode request(final String method, final Object params, final Duration timeout) {
            final String requestId = Long.toString(nextRequestId.getAndIncrement());
            final CompletableFuture<JsonNode> responseFuture = new CompletableFuture<>();
            pendingResponses.put(requestId, responseFuture);
            send(buildRequest(requestId, method, params));
            try {
                final JsonNode response = responseFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (response.has("error")) {
                    final JsonNode error = response.path("error");
                    throw new IllegalStateException("Codex App Server request failed for " + method + ": " + error.path("message").asText("unknown error"));
                }
                return response.path("result");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted waiting for Codex App Server response.", exception);
            } catch (ExecutionException | TimeoutException exception) {
                throw new IllegalStateException("Failed waiting for Codex App Server response for " + method + ".", exception);
            } finally {
                pendingResponses.remove(requestId);
            }
        }

        @Override
        public void notify(final String method, final Object params) {
            send(buildNotification(method, params));
        }

        @Override
        public void close() {
            final WebSocket activeSocket = webSocket;
            if (activeSocket != null) {
                activeSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            }
            try {
                closed.get(2L, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException ignored) {
                // Closing is best effort.
            }
        }

        @Override
        public void onOpen(final WebSocket webSocket) {
            webSocket.request(1L);
        }

        @Override
        public CompletionStage<?> onText(final WebSocket webSocket, final CharSequence data, final boolean last) {
            currentMessage.append(data);
            if (last) {
                final String message = currentMessage.toString();
                currentMessage.setLength(0);
                handleMessage(message);
            }
            webSocket.request(1L);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(final WebSocket webSocket, final int statusCode, final String reason) {
            completePending(new IllegalStateException("Codex App Server socket closed: " + statusCode + " " + reason));
            closed.complete(null);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(final WebSocket webSocket, final Throwable error) {
            completePending(error);
            closed.completeExceptionally(error);
        }

        private void handleMessage(final String payload) {
            try {
                final JsonNode message = objectMapper.readTree(payload);
                if (message.has("id") && (message.has("result") || message.has("error"))) {
                    final CompletableFuture<JsonNode> future = pendingResponses.get(message.path("id").asText());
                    if (future != null) {
                        future.complete(message);
                    }
                    return;
                }
                if (message.has("id") && message.has("method")) {
                    handleServerRequest(message);
                }
            } catch (IOException exception) {
                completePending(exception);
            }
        }

        private void handleServerRequest(final JsonNode request) {
            final String method = request.path("method").asText();
            switch (method) {
                case "item/commandExecution/requestApproval" -> send(buildResult(request.path("id"), DENIED_EXEC_APPROVAL));
                case "item/fileChange/requestApproval" -> send(buildResult(request.path("id"), DECLINED_FILE_CHANGE));
                case "item/tool/requestUserInput" -> send(buildResult(request.path("id"), EMPTY_USER_INPUT));
                case "item/tool/call" -> send(buildResult(request.path("id"), UNSUPPORTED_TOOL_RESPONSE));
                default -> send(buildError(
                    request.path("id"),
                    -32601,
                    "ConstructraOS codex-bridge does not support server request method " + method + "."
                ));
            }
        }

        private void send(final ObjectNode message) {
            final WebSocket activeSocket = webSocket;
            if (activeSocket == null) {
                throw new IllegalStateException("Codex App Server socket is not connected.");
            }
            try {
                activeSocket.sendText(objectMapper.writeValueAsString(message), true)
                    .get(10L, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted sending Codex App Server message.", exception);
            } catch (ExecutionException | TimeoutException | IOException exception) {
                throw new IllegalStateException("Failed sending Codex App Server message.", exception);
            }
        }

        private void completePending(final Throwable error) {
            pendingResponses.values().forEach(future -> future.completeExceptionally(error));
            pendingResponses.clear();
        }

        private ObjectNode buildRequest(final String id, final String method, final Object params) {
            final ObjectNode request = objectMapper.createObjectNode();
            request.put("id", id);
            request.put("method", method);
            request.set("params", objectMapper.valueToTree(params));
            return request;
        }

        private ObjectNode buildNotification(final String method, final Object params) {
            final ObjectNode notification = objectMapper.createObjectNode();
            notification.put("method", method);
            if (params != null) {
                notification.set("params", objectMapper.valueToTree(params));
            }
            return notification;
        }

        private ObjectNode buildResult(final JsonNode id, final Object result) {
            final ObjectNode response = objectMapper.createObjectNode();
            response.set("id", id);
            response.set("result", objectMapper.valueToTree(result));
            return response;
        }

        private ObjectNode buildError(final JsonNode id, final int code, final String message) {
            final ObjectNode response = objectMapper.createObjectNode();
            final ObjectNode error = objectMapper.createObjectNode();
            error.put("code", code);
            error.put("message", message);
            response.set("id", id);
            response.set("error", error);
            return response;
        }
    }
}
