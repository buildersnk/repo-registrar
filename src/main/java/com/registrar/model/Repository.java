package com.registrar.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Repository {

    private String id;
    private String name;
    private String url;
    private String description;
    private String repoType;           // producer | consumer | both
    private String healthStatus;       // healthy | degraded | unhealthy | unknown
    private String healthCheckUrl;
    private Instant lastHealthCheck;
    private List<Map<String, String>> apis;  // [{path, method, description, contentType}]
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;

    public Repository() {
        this.id = UUID.randomUUID().toString();
        this.healthStatus = "unknown";
        this.apis = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRepoType() { return repoType; }
    public void setRepoType(String repoType) { this.repoType = repoType; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

    public String getHealthCheckUrl() { return healthCheckUrl; }
    public void setHealthCheckUrl(String healthCheckUrl) { this.healthCheckUrl = healthCheckUrl; }

    public Instant getLastHealthCheck() { return lastHealthCheck; }
    public void setLastHealthCheck(Instant lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }

    public List<Map<String, String>> getApis() { return apis; }
    public void setApis(List<Map<String, String>> apis) { this.apis = apis != null ? apis : new ArrayList<>(); }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
