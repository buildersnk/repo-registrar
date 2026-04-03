package com.registrar.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.registrar.Database;
import com.registrar.model.Repository;
import com.registrar.util.JsonUtil;
import com.registrar.util.ResponseUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

import static com.registrar.handler.RootHandler.baseUrl;
import static com.registrar.handler.RootHandler.link;

/**
 * Handles:
 *   GET    /repositories/{id}
 *   PUT    /repositories/{id}
 *   DELETE /repositories/{id}
 *   GET    /repositories/{id}/health
 *   POST   /repositories/{id}/heartbeat
 */
public class RepositoryHandler {

    private static final Set<String> VALID_TYPES   = Set.of("producer", "consumer", "both");
    private static final Set<String> VALID_HEALTH  = Set.of("healthy", "degraded", "unhealthy");

    public static void handle(HttpExchange exchange) throws Exception {
        // path: /repositories/{id}  or  /repositories/{id}/health  or  /repositories/{id}/heartbeat
        String[] parts = exchange.getRequestURI().getPath().split("/");
        // parts[0]="", parts[1]="repositories", parts[2]=id, parts[3]=sub-resource (optional)

        if (parts.length < 3 || parts[2].isBlank()) {
            ResponseUtil.sendError(exchange, 404, "Not found");
            return;
        }

        String id          = parts[2];
        String subResource = parts.length > 3 ? parts[3] : null;
        String method      = exchange.getRequestMethod();

        if (subResource == null) {
            switch (method) {
                case "GET"    -> get(exchange, id);
                case "PUT"    -> update(exchange, id);
                case "DELETE" -> delete(exchange, id);
                default       -> ResponseUtil.sendError(exchange, 405, "Method not allowed");
            }
        } else if ("health".equals(subResource) && "GET".equals(method)) {
            health(exchange, id);
        } else if ("heartbeat".equals(subResource) && "POST".equals(method)) {
            heartbeat(exchange, id);
        } else {
            ResponseUtil.sendError(exchange, 404, "Not found");
        }
    }

    private static void get(HttpExchange exchange, String id) throws Exception {
        Optional<Repository> repo = Database.findById(id);
        if (repo.isEmpty()) { ResponseUtil.sendError(exchange, 404, "Repository not found"); return; }
        ResponseUtil.sendJson(exchange, 200, RepositoriesHandler.toMap(repo.get(), baseUrl(exchange)));
    }

    @SuppressWarnings("unchecked")
    private static void update(HttpExchange exchange, String id) throws Exception {
        Map<String, Object> input = JsonUtil.fromJson(
                exchange.getRequestBody().readAllBytes(), new TypeReference<>() {});

        if (input.containsKey("repoType") && !VALID_TYPES.contains(input.get("repoType"))) {
            ResponseUtil.sendError(exchange, 400, "repoType must be one of: producer, consumer, both");
            return;
        }

        Map<String, Object> fields = new HashMap<>();
        for (String key : List.of("url", "description", "repoType", "healthCheckUrl", "apis", "tags")) {
            if (input.containsKey(key)) fields.put(key, input.get(key));
        }

        Optional<Repository> updated = Database.updateFields(id, fields);
        if (updated.isEmpty()) { ResponseUtil.sendError(exchange, 404, "Repository not found"); return; }
        ResponseUtil.sendJson(exchange, 200, RepositoriesHandler.toMap(updated.get(), baseUrl(exchange)));
    }

    private static void delete(HttpExchange exchange, String id) throws Exception {
        if (!Database.delete(id)) { ResponseUtil.sendError(exchange, 404, "Repository not found"); return; }
        ResponseUtil.sendNoContent(exchange);
    }

    private static void health(HttpExchange exchange, String id) throws Exception {
        Optional<Repository> opt = Database.findById(id);
        if (opt.isEmpty()) { ResponseUtil.sendError(exchange, 404, "Repository not found"); return; }

        Repository r    = opt.get();
        String base     = baseUrl(exchange);
        String selfBase = base + "/repositories/" + id;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id",              r.getId());
        response.put("name",            r.getName());
        response.put("healthStatus",    r.getHealthStatus());
        response.put("lastHealthCheck", r.getLastHealthCheck() != null ? r.getLastHealthCheck().toString() : null);
        response.put("healthCheckUrl",  r.getHealthCheckUrl());
        response.put("_links", Map.of(
                "self",       link(selfBase + "/health", "GET"),
                "repository", link(selfBase, "GET"),
                "heartbeat",  link(selfBase + "/heartbeat", "POST")
        ));
        ResponseUtil.sendJson(exchange, 200, response);
    }

    private static void heartbeat(HttpExchange exchange, String id) throws Exception {
        Map<String, Object> input = JsonUtil.fromJson(
                exchange.getRequestBody().readAllBytes(), new TypeReference<>() {});

        String healthStatus = (String) input.get("healthStatus");
        if (healthStatus == null || !VALID_HEALTH.contains(healthStatus)) {
            ResponseUtil.sendError(exchange, 400, "healthStatus must be one of: healthy, degraded, unhealthy");
            return;
        }

        Optional<Repository> updated = Database.updateHealth(id, healthStatus);
        if (updated.isEmpty()) { ResponseUtil.sendError(exchange, 404, "Repository not found"); return; }
        ResponseUtil.sendJson(exchange, 200, RepositoriesHandler.toMap(updated.get(), baseUrl(exchange)));
    }
}
