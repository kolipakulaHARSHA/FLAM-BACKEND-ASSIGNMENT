package com.hars.queuectl;

import com.hars.queuectl.commands.QueueCtlCommand;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new QueueCtlCommand()).execute(args);
        System.exit(exitCode);
    }
}
