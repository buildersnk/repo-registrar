package com.registrar.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ResponseUtil {

    public static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        try {
            byte[] bytes = JsonUtil.toJson(body).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/hal+json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (Exception e) {
            sendPlainError(exchange, 500, "Serialization error");
        } finally {
            exchange.getResponseBody().close();
        }
    }

    public static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        try {
            byte[] bytes = JsonUtil.toJson(Map.of("status", status, "error", message))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (Exception e) {
            sendPlainError(exchange, 500, "error");
        } finally {
            exchange.getResponseBody().close();
        }
    }

    public static void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.getResponseBody().close();
    }

    private static void sendPlainError(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
