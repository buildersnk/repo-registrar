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

public class RepositoriesHandler {

    private static final Set<String> VALID_TYPES = Set.of("producer", "consumer", "both");

    public static void handle(HttpExchange exchange) throws Exception {
        switch (exchange.getRequestMethod()) {
            case "GET"  -> list(exchange);
            case "POST" -> register(exchange);
            default     -> ResponseUtil.sendError(exchange, 405, "Method not allowed");
        }
    }

    private static void list(HttpExchange exchange) throws Exception {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        List<Repository> repos = Database.findAll(params.get("repoType"), params.get("healthStatus"), params.get("tag"));

        String base = baseUrl(exchange);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", repos.size());
        response.put("repositories", repos.stream().map(r -> toMap(r, base)).toList());
        response.put("_links", Map.of(
                "self",     link(base + "/repositories", "GET"),
                "register", link(base + "/repositories", "POST"),
                "stats",    link(base + "/stats", "GET")
        ));
        ResponseUtil.sendJson(exchange, 200, response);
    }

    @SuppressWarnings("unchecked")
    private static void register(HttpExchange exchange) throws Exception {
        Map<String, Object> input = JsonUtil.fromJson(
                exchange.getRequestBody().readAllBytes(), new TypeReference<>() {});

        String name        = (String) input.get("name");
        String description = (String) input.get("description");
        String repoType    = (String) input.get("repoType");

        if (name == null || description == null || repoType == null) {
            ResponseUtil.sendError(exchange, 400, "name, description, and repoType are required");
            return;
        }
        if (!VALID_TYPES.contains(repoType)) {
            ResponseUtil.sendError(exchange, 400, "repoType must be one of: producer, consumer, both");
            return;
        }
        if (Database.existsByName(name)) {
            ResponseUtil.sendError(exchange, 409, "Repository '" + name + "' is already registered");
            return;
        }

        Repository repo = new Repository();
        repo.setName(name);
        repo.setDescription(description);
        repo.setRepoType(repoType);
        repo.setUrl((String) input.get("url"));
        repo.setHealthCheckUrl((String) input.get("healthCheckUrl"));
        if (input.get("apis") instanceof List<?> apis) repo.setApis((List<Map<String, String>>) apis);
        if (input.get("tags") instanceof List<?> tags) repo.setTags((List<String>) tags);

        Database.create(repo);

        String base = baseUrl(exchange);
        ResponseUtil.sendJson(exchange, 201, toMap(repo, base));
    }

    /** Converts a Repository to the HAL+JSON response map with _links. */
    public static Map<String, Object> toMap(Repository repo, String base) {
        String self = base + "/repositories/" + repo.getId();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              repo.getId());
        m.put("name",            repo.getName());
        m.put("url",             repo.getUrl());
        m.put("description",     repo.getDescription());
        m.put("repoType",        repo.getRepoType());
        m.put("healthStatus",    repo.getHealthStatus());
        m.put("healthCheckUrl",  repo.getHealthCheckUrl());
        m.put("lastHealthCheck", repo.getLastHealthCheck() != null ? repo.getLastHealthCheck().toString() : null);
        m.put("apis",            repo.getApis());
        m.put("tags",            repo.getTags());
        m.put("createdAt",       repo.getCreatedAt().toString());
        m.put("updatedAt",       repo.getUpdatedAt().toString());
        m.put("_links", Map.of(
                "self",        link(self, "GET"),
                "collection",  link(base + "/repositories", "GET"),
                "health",      link(self + "/health", "GET"),
                "heartbeat",   link(self + "/heartbeat", "POST"),
                "update",      link(self, "PUT"),
                "deregister",  link(self, "DELETE")
        ));
        return m;
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) params.put(kv[0], kv[1]);
        }
        return params;
    }
}
