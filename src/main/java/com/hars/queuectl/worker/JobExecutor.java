package com.hars.queuectl.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.hars.queuectl.model.Job;

public class JobExecutor {
    
    /**
     * Executes a job's command and returns the exit code
     * @param job The job to execute
     * @return Exit code (0 = success, non-zero = failure)
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
            
            // Read and print output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Job " + job.getId() + "] " + line);
                }
            }
            
            // Wait for the process to complete
            int exitCode = process.waitFor();
            
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
