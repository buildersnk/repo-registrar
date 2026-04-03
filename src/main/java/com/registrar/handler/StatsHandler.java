package com.registrar.handler;

import com.registrar.Database;
import com.registrar.util.ResponseUtil;
import com.sun.net.httpserver.HttpExchange;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.registrar.handler.RootHandler.baseUrl;
import static com.registrar.handler.RootHandler.link;

public class StatsHandler {

    public static void handle(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            ResponseUtil.sendError(exchange, 405, "Method not allowed");
            return;
        }

        Map<String, Object> stats = Database.getStats();
        String base = baseUrl(exchange);

        Map<String, Object> response = new LinkedHashMap<>(stats);
        response.put("_links", Map.of(
                "self",           link(base + "/stats", "GET"),
                "repositories",   link(base + "/repositories", "GET"),
                "healthyRepos",   link(base + "/repositories?healthStatus=healthy", "GET"),
                "unhealthyRepos", link(base + "/repositories?healthStatus=unhealthy", "GET"),
                "degradedRepos",  link(base + "/repositories?healthStatus=degraded", "GET"),
                "producers",      link(base + "/repositories?repoType=producer", "GET"),
                "consumers",      link(base + "/repositories?repoType=consumer", "GET"),
                "both",           link(base + "/repositories?repoType=both", "GET")
        ));
        ResponseUtil.sendJson(exchange, 200, response);
    }
}
