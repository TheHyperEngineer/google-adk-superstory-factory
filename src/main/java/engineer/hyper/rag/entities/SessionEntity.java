package engineer.hyper.rag.entities;


import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sessions", indexes = {
        @Index(name = "idx_sessions_app_user", columnList = "app_name,user_id")
})
public class SessionEntity {
    @Id
    @Column(length = 255, nullable = false)
    private String id;

    @Column(name = "app_name", length = 255, nullable = false)
    private String appName;

    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Lob
    @Column(name = "state", nullable = false)
    private String state; // JSON serialized

    @Column(name = "last_update_time")
    private Instant lastUpdateTime;

    // Constructors, getters, setters
    public SessionEntity() {
    }

    public SessionEntity(String id, String appName, String userId, String state, Instant lastUpdateTime) {
        this.id = id;
        this.appName = appName;
        this.userId = userId;
        this.state = state;
        this.lastUpdateTime = lastUpdateTime;
    }

    // Getters and setters...
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Instant lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}