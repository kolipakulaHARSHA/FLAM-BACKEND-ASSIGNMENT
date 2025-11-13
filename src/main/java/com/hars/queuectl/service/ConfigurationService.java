package com.hars.queuectl.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ConfigurationService {
    
    private static final String CONFIG_FILE = "config.json";
    private final ObjectMapper objectMapper;
    
    public ConfigurationService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Initialize the configuration file
     */
    public void initialize() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            // Create default configuration
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("max-retries", 3);
            defaultConfig.put("poll-interval-ms", 500);
            defaultConfig.put("base-backoff-ms", 1000);
            saveConfig(defaultConfig);
        }
    }
    
    /**
     * Get a configuration value
     */
    public Object get(String key) {
        Map<String, Object> config = loadConfig();
        return config.get(key);
    }
    
    /**
     * Set a configuration value
     */
    public void set(String key, Object value) {
        Map<String, Object> config = loadConfig();
        config.put(key, value);
        saveConfig(config);
    }
    
    /**
     * Get all configuration
     */
    public Map<String, Object> getAll() {
        return loadConfig();
    }
    
    /**
     * Load configuration from file
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadConfig() {
        try {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) {
                return new HashMap<>();
            }
            return objectMapper.readValue(file, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
    
    /**
     * Save configuration to file
     */
    private void saveConfig(Map<String, Object> config) {
        try {
            objectMapper.writeValue(new File(CONFIG_FILE), config);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save configuration", e);
        }
    }
}
