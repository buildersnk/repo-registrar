package com.registrar;

import com.registrar.model.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Seeds realistic demo data so the Swagger UI is immediately interactive.
 * Only runs when the database is empty.
 */
public class DataSeeder {

    public static void seed() throws Exception {
        if (!Database.findAll(null, null, null).isEmpty()) return;

        record Seed(String name, String desc, String type, String health, List<String> tags,
                    List<Map<String, String>> apis) {}

        List<Seed> seeds = List.of(
            new Seed(
                "payment-service",
                "Processes payments and emits transaction events to the ledger topic",
                "producer", "healthy",
                List.of("payments", "finance", "events"),
                List.of(
                    Map.of("path", "/payments", "method", "POST", "description", "Initiate a payment", "contentType", "application/json"),
                    Map.of("path", "/payments/{id}", "method", "GET", "description", "Get payment status", "contentType", "application/json"),
                    Map.of("path", "/payments/{id}/refund", "method", "POST", "description", "Issue a refund", "contentType", "application/json")
                )
            ),
            new Seed(
                "order-service",
                "Manages the full order lifecycle; consumes inventory events and produces order events",
                "both", "healthy",
                List.of("orders", "core"),
                List.of(
                    Map.of("path", "/orders", "method", "POST", "description", "Place a new order", "contentType", "application/json"),
                    Map.of("path", "/orders", "method", "GET", "description", "List orders", "contentType", "application/json"),
                    Map.of("path", "/orders/{id}", "method", "GET", "description", "Get order details", "contentType", "application/json"),
                    Map.of("path", "/orders/{id}/cancel", "method", "POST", "description", "Cancel an order", "contentType", "application/json")
                )
            ),
            new Seed(
                "inventory-service",
                "Tracks product stock levels and emits stock-change events",
                "producer", "healthy",
                List.of("inventory", "warehouse"),
                List.of(
                    Map.of("path", "/products", "method", "GET", "description", "List all products", "contentType", "application/json"),
                    Map.of("path", "/products/{id}/stock", "method", "GET", "description", "Get stock level", "contentType", "application/json"),
                    Map.of("path", "/products/{id}/stock", "method", "PUT", "description", "Update stock level", "contentType", "application/json")
                )
            ),
            new Seed(
                "notification-service",
                "Consumes order and payment events to send email, SMS, and push notifications",
                "consumer", "degraded",
                List.of("notifications", "email", "sms"),
                List.of(
                    Map.of("path", "/notifications/preferences/{userId}", "method", "GET", "description", "Get user notification prefs", "contentType", "application/json"),
                    Map.of("path", "/notifications/preferences/{userId}", "method", "PUT", "description", "Update prefs", "contentType", "application/json")
                )
            ),
            new Seed(
                "analytics-service",
                "Consumes all domain events to build business intelligence dashboards and reports",
                "consumer", "healthy",
                List.of("analytics", "reporting", "bi"),
                List.of(
                    Map.of("path", "/reports/daily", "method", "GET", "description", "Daily summary", "contentType", "application/json"),
                    Map.of("path", "/reports/revenue", "method", "GET", "description", "Revenue breakdown", "contentType", "application/json")
                )
            ),
            new Seed(
                "user-service",
                "Manages user accounts, authentication, and profile data",
                "producer", "unhealthy",
                List.of("auth", "users", "core"),
                List.of(
                    Map.of("path", "/users", "method", "POST", "description", "Register user", "contentType", "application/json"),
                    Map.of("path", "/users/{id}", "method", "GET", "description", "Get user profile", "contentType", "application/json"),
                    Map.of("path", "/auth/token", "method", "POST", "description", "Issue JWT token", "contentType", "application/json")
                )
            )
        );

        Instant now = Instant.now();
        for (Seed s : seeds) {
            Repository repo = new Repository();
            repo.setName(s.name());
            repo.setDescription(s.desc());
            repo.setRepoType(s.type());
            repo.setHealthStatus(s.health());
            repo.setTags(s.tags());
            repo.setApis(s.apis());
            repo.setUrl("https://github.com/acme/" + s.name());
            repo.setHealthCheckUrl("https://" + s.name() + ".acme.io/health");
            if (!"unknown".equals(s.health())) {
                repo.setLastHealthCheck(now);
            }
            Database.create(repo);
        }

        System.out.println("Demo data seeded: " + seeds.size() + " repositories");
    }
}
