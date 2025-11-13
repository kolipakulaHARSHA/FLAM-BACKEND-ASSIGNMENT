package com.hars.queuectl.commands;

import com.hars.queuectl.service.ConfigurationService;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "config",
    description = "Configure settings"
)
public class ConfigSetCommand implements Runnable {
    
    @Parameters(index = "0", description = "Configuration key")
    private String key;
    
    @Parameters(index = "1", description = "Configuration value")
    private String value;
    
    @Override
    public void run() {
        ConfigurationService configService = new ConfigurationService();
        configService.initialize();
        
        // Try to parse value as integer
        Object configValue;
        try {
            configValue = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Keep as string
            configValue = value;
        }
        
        configService.set(key, configValue);
        
        System.out.println("Configuration updated: " + key + " = " + configValue);
    }
}
