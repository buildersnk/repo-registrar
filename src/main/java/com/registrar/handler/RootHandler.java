package com.registrar.handler;

import com.registrar.util.ResponseUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class RootHandler {

    public static void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            ResponseUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }
        String base = baseUrl(exchange);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", "Repository Registrar");
        response.put("description", "Central registry for discovering, tracking, and monitoring repositories in the ecosystem");
        response.put("version", "1.0.0");
        response.put("_links", Map.of(
                "self",         link(base + "/", "GET"),
                "repositories", link(base + "/repositories", "GET"),
                "register",     link(base + "/repositories", "POST"),
                "stats",        link(base + "/stats", "GET")
        ));
        ResponseUtil.sendJson(exchange, 200, response);
    }

    // Shared helpers used by all handlers
    static String baseUrl(HttpExchange exchange) {
        String host = exchange.getRequestHeaders().getFirst("Host");
        return "http://" + host;
    }

    static Map<String, String> link(String href, String method) {
        return Map.of("href", href, "method", method);
    }
}
