package com.hars.queuectl.commands;

import picocli.CommandLine.Command;

@Command(
    name = "dlq",
    description = "Dead Letter Queue management",
    subcommands = {
        DLQListCommand.class,
        DLQRetryCommand.class
    }
)
public class DLQCommand implements Runnable {
    
    @Override
    public void run() {
        // Show help by default
        picocli.CommandLine.usage(this, System.out);
    }
}
