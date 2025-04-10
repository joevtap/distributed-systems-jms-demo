package com.joevtap.jmsapp;

import java.io.File;

/**
 * Configuration class that loads settings from system environment variables
 */
public class Config {
    private static Config instance;

    // Connection settings
    private final String brokerUrl;
    private final String username;
    private final String password;

    // Queue names
    private final String queueName;
    private final String replyQueueName;

    // File directory
    private final String fileDirectory;

    private Config() {
        // Load connection settings with fallback defaults
        brokerUrl = getEnv("ACTIVEMQ_BROKER_URL", "tcp://localhost:61616");
        username = getEnv("ACTIVEMQ_USERNAME", "artemis");
        password = getEnv("ACTIVEMQ_PASSWORD", "artemis");

        // Load queue names with fallback defaults
        queueName = getEnv("QUEUE_NAME", "app.events");
        replyQueueName = getEnv("REPLY_QUEUE_NAME", "app.responses");

        // Load file directory with fallback default
        fileDirectory = getEnv("FILE_DIRECTORY", "./files/");

        // Ensure file directory exists
        File dir = new File(fileDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Log the configuration
        System.out.println("Configuration loaded:");
        System.out.println("- Broker URL: " + brokerUrl);
        System.out.println("- Username: " + username);
        System.out.println("- Queue: " + queueName);
        System.out.println("- Reply Queue: " + replyQueueName);
        System.out.println("- File Directory: " + fileDirectory);
    }

    /**
     * Get configuration value from environment with fallback default
     */
    private String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get singleton instance of Config
     */
    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    // Getter methods
    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getReplyQueueName() {
        return replyQueueName;
    }

    public String getFileDirectory() {
        return fileDirectory;
    }
}