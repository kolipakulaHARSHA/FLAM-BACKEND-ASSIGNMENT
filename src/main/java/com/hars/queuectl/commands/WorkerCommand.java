package com.hars.queuectl.commands;

import picocli.CommandLine.Command;

@Command(
    name = "worker",
    description = "Manage worker processes",
    subcommands = {
        WorkerStartCommand.class,
        WorkerStopCommand.class
    }
)
public class WorkerCommand implements Runnable {
    
    @Override
    public void run() {
        // Show help by default
        picocli.CommandLine.usage(this, System.out);
    }
}
