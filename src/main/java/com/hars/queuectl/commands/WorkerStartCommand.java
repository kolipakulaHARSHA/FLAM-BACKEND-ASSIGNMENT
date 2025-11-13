package com.hars.queuectl.commands;

import com.hars.queuectl.service.JobRepository;
import com.hars.queuectl.service.WorkerService;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "worker",
    description = "Start worker processes"
)
public class WorkerStartCommand implements Runnable {
    
    @Option(names = {"--count", "-c"}, description = "Number of worker threads", defaultValue = "1")
    private int count;
    
    @Override
    public void run() {
        JobRepository jobRepository = new JobRepository();
        jobRepository.initialize();
        
        WorkerService workerService = new WorkerService(jobRepository);
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            workerService.stop();
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
    }
}
