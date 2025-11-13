package com.hars.queuectl.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import com.hars.queuectl.model.Job;

public class JobExecutor {
    
    // Special exit code for timeout
    public static final int EXIT_CODE_TIMEOUT = -2;
    
    /**
     * Executes a job's command and returns the exit code
     * @param job The job to execute
     * @return Exit code (0 = success, non-zero = failure, -2 = timeout)
     */
    public int execute(Job job) {
        try {
            System.out.println("Executing job " + job.getId() + ": " + job.getCommand());
            
            // Determine the shell based on the OS
            ProcessBuilder processBuilder;
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                // Windows
                processBuilder = new ProcessBuilder("cmd.exe", "/c", job.getCommand());
            } else {
                // Unix-like (Linux, Mac)
                processBuilder = new ProcessBuilder("sh", "-c", job.getCommand());
            }
            
            // Redirect error stream to output stream
            processBuilder.redirectErrorStream(true);
            
            // Start the process
            Process process = processBuilder.start();
            
            // Read output in a separate thread to avoid blocking
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Job " + job.getId() + "] " + line);
                    }
                } catch (IOException e) {
                    // Ignore - process was likely terminated
                }
            });
            outputThread.setDaemon(true);
            outputThread.start();
            
            // Wait for the process to complete with timeout
            long timeout = job.getTimeoutSeconds() > 0 ? job.getTimeoutSeconds() : 300; // Default 5 minutes
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            
            if (!finished) {
                // Timeout occurred - attempt graceful termination
                System.err.println("Job " + job.getId() + " exceeded timeout of " + timeout + " seconds. Terminating...");
                
                // First try graceful termination (SIGTERM)
                process.destroy();
                
                // Wait up to 5 seconds for graceful shutdown
                boolean gracefulShutdown = process.waitFor(5, TimeUnit.SECONDS);
                
                if (!gracefulShutdown) {
                    // Force kill if still running (SIGKILL)
                    System.err.println("Job " + job.getId() + " did not terminate gracefully. Force killing...");
                    process.destroyForcibly();
                    process.waitFor(); // Wait for forced termination
                }
                
                return EXIT_CODE_TIMEOUT;
            }
            
            // Wait for the process to complete
            int exitCode = process.exitValue();
            
            System.out.println("Job " + job.getId() + " finished with exit code: " + exitCode);
            
            return exitCode;
            
        } catch (IOException e) {
            System.err.println("Failed to execute job " + job.getId() + ": " + e.getMessage());
            return -1;
        } catch (InterruptedException e) {
            System.err.println("Job " + job.getId() + " was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}
