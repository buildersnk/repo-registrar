package com.registrar;

import com.registrar.handler.RepositoriesHandler;
import com.registrar.handler.RepositoryHandler;
import com.registrar.handler.RootHandler;
import com.registrar.handler.StatsHandler;
import com.registrar.handler.SwaggerHandler;
import com.registrar.util.ResponseUtil;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws Exception {
        Database.init();
        DataSeeder.seed();

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            try {
                if (path.equals("/")) {
                    RootHandler.handle(exchange);
                } else if (path.equals("/swagger-ui") || path.equals("/swagger-ui/")) {
                    SwaggerHandler.handleUi(exchange);
                } else if (path.equals("/openapi.json")) {
                    SwaggerHandler.handleSpec(exchange);
                } else if (path.equals("/stats")) {
                    StatsHandler.handle(exchange);
                } else if (path.equals("/repositories") || path.equals("/repositories/")) {
                    RepositoriesHandler.handle(exchange);
                } else if (path.startsWith("/repositories/")) {
                    RepositoryHandler.handle(exchange);
                } else {
                    ResponseUtil.sendError(exchange, 404, "Not found");
                }
            } catch (Exception e) {
                try {
                    ResponseUtil.sendError(exchange, 500, "Internal server error: " + e.getMessage());
                } catch (Exception ignored) {}
            }
        });

        server.start();
        System.out.println("Repository Registrar running on http://localhost:" + port);
        System.out.println("  GET  /swagger-ui                - Interactive API explorer");
        System.out.println("  GET  /openapi.json              - OpenAPI 3.0 spec");
        System.out.println("  GET  /repositories              - List all repositories");
        System.out.println("  POST /repositories              - Register a new repository");
        System.out.println("  GET  /repositories/{id}         - Get repository details");
        System.out.println("  PUT  /repositories/{id}         - Update repository");
        System.out.println("  DELETE /repositories/{id}       - Deregister repository");
        System.out.println("  GET  /repositories/{id}/health  - Get health status");
        System.out.println("  POST /repositories/{id}/heartbeat - Update health status");
        System.out.println("  GET  /stats                     - Ecosystem statistics");
    }
}
