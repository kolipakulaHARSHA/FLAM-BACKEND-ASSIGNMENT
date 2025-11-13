package com.hars.queuectl.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.hars.queuectl.service.JobRepository;
import com.hars.queuectl.service.WorkerService;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "start",
    description = "Start worker processes"
)
public class WorkerStartCommand implements Runnable {
    
    private static final String PID_FILE = "worker.pid";
    
    @Option(names = {"--count", "-c"}, description = "Number of worker threads", defaultValue = "1")
    private int count;
    
    @Override
    public void run() {
        // Check if workers are already running
        File pidFile = new File(PID_FILE);
        if (pidFile.exists()) {
            System.err.println("Workers are already running. Use 'queuectl worker stop' to stop them first.");
            System.exit(1);
        }
        
        // Write current process PID to file
        long pid = ProcessHandle.current().pid();
        try (FileWriter writer = new FileWriter(PID_FILE)) {
            writer.write(String.valueOf(pid));
            System.out.println("Worker process started with PID: " + pid);
        } catch (IOException e) {
            System.err.println("Failed to write PID file: " + e.getMessage());
            System.exit(1);
        }
        
        JobRepository jobRepository = new JobRepository();
        jobRepository.initialize();
        
        WorkerService workerService = new WorkerService(jobRepository);
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            workerService.stop();
            // Clean up PID file
            pidFile.delete();
        }));
        
        // Start workers
        workerService.start(count);
        
        // Keep the main thread alive
        try {
            while (workerService.isRunning()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Clean up PID file on normal exit
        pidFile.delete();
    }
}
