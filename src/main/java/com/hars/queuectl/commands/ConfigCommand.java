package com.hars.queuectl.commands;

import picocli.CommandLine.Command;

@Command(
    name = "config",
    description = "Configuration management",
    subcommands = {
        ConfigSetCommand.class
    }
)
public class ConfigCommand implements Runnable {
    
    @Override
    public void run() {
        // Show help by default
        picocli.CommandLine.usage(this, System.out);
    }
}
