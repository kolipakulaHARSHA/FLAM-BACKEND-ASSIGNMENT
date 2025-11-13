package com.hars.queuectl.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Job {
    public enum JobState {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        DEAD
    }

    @JsonProperty("id")
    private String id;

    @JsonProperty("command")
    private String command;

    @JsonProperty("state")
    private JobState state;

    @JsonProperty("attempts")
    private int attempts;

    @JsonProperty("max_retries")
    private int maxRetries;

    @JsonProperty("timeout_seconds")
    private long timeoutSeconds;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    // Default constructor for Jackson
    public Job() {
    }

    // Constructor with parameters
    public Job(String id, String command, JobState state, int attempts, int maxRetries, long timeoutSeconds, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.command = command;
        this.state = state;
        this.attempts = attempts;
        this.maxRetries = maxRetries;
        this.timeoutSeconds = timeoutSeconds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Job{" +
                "id='" + id + '\'' +
                ", command='" + command + '\'' +
                ", state=" + state +
                ", attempts=" + attempts +
                ", maxRetries=" + maxRetries +
                ", timeoutSeconds=" + timeoutSeconds +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
