package engineer.hyper.rag.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "memory_entries", indexes = {
    @Index(name = "idx_memory_app_user", columnList = "app_name,user_id"),
    @Index(name = "idx_memory_session", columnList = "session_id"),
    @Index(name = "idx_memory_timestamp", columnList = "timestamp")
})
public class MemoryEntryEntity {
    @Id
    @Column(length = 255, nullable = false)
    private String id;
    
    @Column(name = "app_name", length = 255, nullable = false)
    private String appName;
    
    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;
    
    @Column(name = "session_id", length = 255, nullable = false)
    private String sessionId;
    
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "CLOB")
    private String content;
    
    @Column(name = "author", length = 255)
    private String author;
    
    @Column(name = "timestamp", length = 100)
    private String timestamp;
    
    @Column(name = "search_text", length = 4000)
    private String searchText;
    
    public MemoryEntryEntity() {}
    
    public MemoryEntryEntity(String id, String appName, String userId, String sessionId, 
                             String content, String author, String timestamp, String searchText) {
        this.id = id;
        this.appName = appName;
        this.userId = userId;
        this.sessionId = sessionId;
        this.content = content;
        this.author = author;
        this.timestamp = timestamp;
        this.searchText = searchText;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
}