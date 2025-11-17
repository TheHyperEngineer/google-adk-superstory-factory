package engineer.hyper.rag.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_events_session", columnList = "session_id")
})
public class EventEntity {
    @Id
    @Column(length = 255, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionEntity session;

    @Column(name = "invocation_id", length = 255)
    private String invocationId;

    @Column(name = "author", length = 255)
    private String author;

    @Lob
    @Column(name = "content")
    private String content;

    @Lob
    @Column(name = "actions")
    private String actions;

    @Lob
    @Column(name = "long_running_tool_ids")
    private String longRunningToolIds;

    @Column(name = "partial")
    private Boolean partial;

    @Column(name = "turn_complete")
    private Boolean turnComplete;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "interrupted")
    private Boolean interrupted;

    @Column(name = "branch", length = 255)
    private String branch;

    @Lob
    @Column(name = "grounding_metadata")
    private String groundingMetadata;

    @Column(name = "event_timestamp")
    private Long timestamp;

    // Constructors, getters, setters...
    public EventEntity() {
    }

    // Getters and setters...
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SessionEntity getSession() {
        return session;
    }

    public void setSession(SessionEntity session) {
        this.session = session;
    }

    public String getInvocationId() {
        return invocationId;
    }

    public void setInvocationId(String invocationId) {
        this.invocationId = invocationId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public String getLongRunningToolIds() {
        return longRunningToolIds;
    }

    public void setLongRunningToolIds(String longRunningToolIds) {
        this.longRunningToolIds = longRunningToolIds;
    }

    public Boolean getPartial() {
        return partial;
    }

    public void setPartial(Boolean partial) {
        this.partial = partial;
    }

    public Boolean getTurnComplete() {
        return turnComplete;
    }

    public void setTurnComplete(Boolean turnComplete) {
        this.turnComplete = turnComplete;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Boolean getInterrupted() {
        return interrupted;
    }

    public void setInterrupted(Boolean interrupted) {
        this.interrupted = interrupted;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getGroundingMetadata() {
        return groundingMetadata;
    }

    public void setGroundingMetadata(String groundingMetadata) {
        this.groundingMetadata = groundingMetadata;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}