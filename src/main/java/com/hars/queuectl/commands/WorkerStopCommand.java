package com.hars.queuectl.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import picocli.CommandLine.Command;

@Command(
    name = "stop",
    description = "Stop running workers gracefully"
)
public class WorkerStopCommand implements Runnable {
    
    private static final String PID_FILE = "worker.pid";
    private static final String STOP_SIGNAL_FILE = "worker.stop";
    
    @Override
    public void run() {
        File pidFile = new File(PID_FILE);
        
        if (!pidFile.exists()) {
            System.out.println("No workers are currently running (PID file not found)");
            return;
        }
        
        try {
            // Read the PID from file
            String pidStr = new String(Files.readAllBytes(Paths.get(PID_FILE))).trim();
            long pid = Long.parseLong(pidStr);
            
            System.out.println("Requesting graceful shutdown of worker process (PID: " + pid + ")...");
            
            // Check if process is still alive
            ProcessHandle processHandle = ProcessHandle.of(pid).orElse(null);
            
            if (processHandle == null || !processHandle.isAlive()) {
                System.out.println("Worker process is not running (stale PID file)");
                pidFile.delete();
                return;
            }
            
            // Create stop signal file for graceful shutdown
            File stopSignalFile = new File(STOP_SIGNAL_FILE);
            try (FileWriter writer = new FileWriter(stopSignalFile)) {
                writer.write("stop");
            }
            
            System.out.println("Stop signal sent. Waiting for worker to finish current job...");
            
            // Wait for the process to stop gracefully (up to 60 seconds)
            int maxWait = 60;
            int waited = 0;
            while (processHandle.isAlive() && waited < maxWait) {
                Thread.sleep(1000);
                waited++;
                if (waited % 5 == 0) {
                    System.out.println("Still waiting... (" + waited + "s)");
                }
            }
            
            if (!processHandle.isAlive()) {
                System.out.println("Workers stopped gracefully");
                pidFile.delete();
                stopSignalFile.delete();
            } else {
                System.out.println("Worker did not stop within " + maxWait + " seconds. Forcing shutdown...");
                processHandle.destroyForcibly();
                
                // Wait a bit more
                Thread.sleep(2000);
                
                if (!processHandle.isAlive()) {
                    System.out.println("Workers force stopped");
                    pidFile.delete();
                    stopSignalFile.delete();
                } else {
                    System.err.println("Failed to stop workers. You may need to kill the process manually.");
                }
            }
            
        } catch (IOException e) {
            System.err.println("Failed to read PID file: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Invalid PID in file: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for workers to stop");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error stopping workers: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
