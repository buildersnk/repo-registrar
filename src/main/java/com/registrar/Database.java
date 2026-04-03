package com.registrar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.registrar.model.Repository;
import com.registrar.util.JsonUtil;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class Database {

    private static final String DB_URL = "jdbc:sqlite:registrar.db";

    public static void init() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS repositories (
                        id                TEXT PRIMARY KEY,
                        name              TEXT UNIQUE NOT NULL,
                        url               TEXT,
                        description       TEXT NOT NULL,
                        repo_type         TEXT NOT NULL,
                        health_status     TEXT NOT NULL DEFAULT 'unknown',
                        health_check_url  TEXT,
                        last_health_check TEXT,
                        apis              TEXT NOT NULL DEFAULT '[]',
                        tags              TEXT NOT NULL DEFAULT '[]',
                        created_at        TEXT NOT NULL,
                        updated_at        TEXT NOT NULL
                    )""");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static List<Repository> findAll(String repoType, String healthStatus, String tag) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT * FROM repositories WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (repoType != null) { sql.append(" AND repo_type = ?"); params.add(repoType); }
        if (healthStatus != null) { sql.append(" AND health_status = ?"); params.add(healthStatus); }
        sql.append(" ORDER BY created_at DESC");

        List<Repository> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Repository r = fromResultSet(rs);
                if (tag == null || r.getTags().contains(tag)) result.add(r);
            }
        }
        return result;
    }

    public static Optional<Repository> findById(String id) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM repositories WHERE id = ?")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(fromResultSet(rs));
        }
        return Optional.empty();
    }

    public static boolean existsByName(String name) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM repositories WHERE name = ?")) {
            ps.setString(1, name);
            return ps.executeQuery().next();
        }
    }

    public static Repository create(Repository repo) throws Exception {
        String sql = """
                INSERT INTO repositories
                    (id, name, url, description, repo_type, health_status,
                     health_check_url, last_health_check, apis, tags, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, repo.getId());
            ps.setString(2, repo.getName());
            ps.setString(3, repo.getUrl());
            ps.setString(4, repo.getDescription());
            ps.setString(5, repo.getRepoType());
            ps.setString(6, repo.getHealthStatus());
            ps.setString(7, repo.getHealthCheckUrl());
            ps.setString(8, repo.getLastHealthCheck() != null ? repo.getLastHealthCheck().toString() : null);
            ps.setString(9, JsonUtil.toJson(repo.getApis()));
            ps.setString(10, JsonUtil.toJson(repo.getTags()));
            ps.setString(11, repo.getCreatedAt().toString());
            ps.setString(12, repo.getUpdatedAt().toString());
            ps.executeUpdate();
        }
        return repo;
    }

    @SuppressWarnings("unchecked")
    public static Optional<Repository> updateFields(String id, Map<String, Object> fields) throws Exception {
        Optional<Repository> existing = findById(id);
        if (existing.isEmpty()) return Optional.empty();

        Repository repo = existing.get();
        if (fields.containsKey("url")) repo.setUrl((String) fields.get("url"));
        if (fields.containsKey("description")) repo.setDescription((String) fields.get("description"));
        if (fields.containsKey("repoType")) repo.setRepoType((String) fields.get("repoType"));
        if (fields.containsKey("healthCheckUrl")) repo.setHealthCheckUrl((String) fields.get("healthCheckUrl"));
        if (fields.containsKey("apis")) repo.setApis((List<Map<String, String>>) fields.get("apis"));
        if (fields.containsKey("tags")) repo.setTags((List<String>) fields.get("tags"));
        repo.setUpdatedAt(Instant.now());

        String sql = """
                UPDATE repositories
                SET url=?, description=?, repo_type=?, health_check_url=?,
                    apis=?, tags=?, updated_at=?
                WHERE id=?""";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, repo.getUrl());
            ps.setString(2, repo.getDescription());
            ps.setString(3, repo.getRepoType());
            ps.setString(4, repo.getHealthCheckUrl());
            ps.setString(5, JsonUtil.toJson(repo.getApis()));
            ps.setString(6, JsonUtil.toJson(repo.getTags()));
            ps.setString(7, repo.getUpdatedAt().toString());
            ps.setString(8, id);
            ps.executeUpdate();
        }
        return Optional.of(repo);
    }

    public static boolean delete(String id) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM repositories WHERE id = ?")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public static Optional<Repository> updateHealth(String id, String healthStatus) throws Exception {
        Instant now = Instant.now();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE repositories SET health_status=?, last_health_check=?, updated_at=? WHERE id=?")) {
            ps.setString(1, healthStatus);
            ps.setString(2, now.toString());
            ps.setString(3, now.toString());
            ps.setString(4, id);
            if (ps.executeUpdate() == 0) return Optional.empty();
        }
        return findById(id);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Map<String, Object> getStats() throws Exception {
        try (Connection conn = getConnection()) {
            Map<String, Object> stats = new LinkedHashMap<>();

            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM repositories");
                stats.put("totalRepositories", rs.getInt(1));
            }

            Map<String, Integer> byType = new LinkedHashMap<>();
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                        "SELECT repo_type, COUNT(*) FROM repositories GROUP BY repo_type");
                while (rs.next()) byType.put(rs.getString(1), rs.getInt(2));
            }
            stats.put("byType", byType);

            Map<String, Integer> byHealth = new LinkedHashMap<>();
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                        "SELECT health_status, COUNT(*) FROM repositories GROUP BY health_status");
                while (rs.next()) byHealth.put(rs.getString(1), rs.getInt(2));
            }
            stats.put("byHealth", byHealth);

            int totalApis = 0;
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT apis FROM repositories");
                while (rs.next()) {
                    List list = JsonUtil.fromJson(rs.getString(1), List.class);
                    totalApis += list.size();
                }
            }
            stats.put("totalApisExposed", totalApis);

            return stats;
        }
    }

    private static Repository fromResultSet(ResultSet rs) throws Exception {
        Repository r = new Repository();
        r.setId(rs.getString("id"));
        r.setName(rs.getString("name"));
        r.setUrl(rs.getString("url"));
        r.setDescription(rs.getString("description"));
        r.setRepoType(rs.getString("repo_type"));
        r.setHealthStatus(rs.getString("health_status"));
        r.setHealthCheckUrl(rs.getString("health_check_url"));
        String lhc = rs.getString("last_health_check");
        r.setLastHealthCheck(lhc != null ? Instant.parse(lhc) : null);
        r.setApis(JsonUtil.fromJson(rs.getString("apis"), new TypeReference<>() {}));
        r.setTags(JsonUtil.fromJson(rs.getString("tags"), new TypeReference<>() {}));
        r.setCreatedAt(Instant.parse(rs.getString("created_at")));
        r.setUpdatedAt(Instant.parse(rs.getString("updated_at")));
        return r;
    }
}
