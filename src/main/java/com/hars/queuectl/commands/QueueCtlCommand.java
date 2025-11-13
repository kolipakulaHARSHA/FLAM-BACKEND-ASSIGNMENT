package com.hars.queuectl.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "queuectl",
    description = "A background job queue system",
    mixinStandardHelpOptions = true,
    version = "1.0",
    subcommands = {
        EnqueueCommand.class,
        WorkerCommand.class,
        StatusCommand.class,
        ListCommand.class,
        DLQCommand.class,
        ConfigCommand.class
    }
)
public class QueueCtlCommand implements Runnable {
    
    @Override
    public void run() {
        // Show help by default
        CommandLine.usage(this, System.out);
    }
}
