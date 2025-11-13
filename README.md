# queuectl - Background Job Queue System

A robust, file-based background job queue system implemented in Java. This system allows you to enqueue shell commands as jobs and process them asynchronously with configurable retry logic and dead letter queue support.

## Features

- **Job Queue Management**: Enqueue shell commands as jobs with configurable retry policies
- **Worker Threads**: Multiple concurrent workers to process jobs in parallel
- **Retry Logic**: Automatic retry with exponential backoff for failed jobs
- **Dead Letter Queue (DLQ)**: Failed jobs are moved to DLQ after max retries
- **State Management**: Track job states (PENDING, PROCESSING, COMPLETED, FAILED, DEAD)
- **Thread-Safe Persistence**: File-based storage with concurrent access control
- **Configuration**: Runtime configuration for system parameters
- **CLI Interface**: Easy-to-use command-line interface built with Picocli

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

## Installation

1. Clone the repository:
```bash
cd flam-backend-assignment
```

2. Build the project:
```bash
mvn clean package
```

This will create an executable JAR file in the `target` directory:
`target/flam-backend-assignment-1.0-SNAPSHOT.jar`

## Usage

### Basic Command Structure

```bash
java -jar target/flam-backend-assignment-1.0-SNAPSHOT.jar <command> [options]
```

For convenience, you can create an alias:
```bash
alias queuectl='java -jar target/flam-backend-assignment-1.0-SNAPSHOT.jar'
```

### Commands

#### 1. Enqueue a Job

Add a new job to the queue:

```bash
queuectl enqueue '{"command":"echo Hello World","max_retries":3}'
```

Job JSON format:
- `command` (required): Shell command to execute
- `max_retries` (optional, default: 3): Maximum retry attempts
- `id` (optional): Job ID (auto-generated if not provided)

Examples:
```bash
# Simple echo command
queuectl enqueue '{"command":"echo Testing","max_retries":3}'

# Run a script
queuectl enqueue '{"command":"bash script.sh","max_retries":2}'

# Failing command (for testing)
queuectl enqueue '{"command":"exit 1","max_retries":2}'
```

#### 2. Start Workers

Start worker threads to process jobs:

```bash
queuectl worker --count 2
```

Options:
- `--count` or `-c`: Number of worker threads (default: 1)

The worker process runs continuously until stopped (Ctrl+C).

#### 3. Check Queue Status

View the current status of all jobs:

```bash
queuectl status
```

Output shows count of jobs in each state:
- PENDING: Jobs waiting to be processed
- PROCESSING: Jobs currently being executed
- COMPLETED: Successfully completed jobs
- FAILED: Jobs that failed but will be retried
- DEAD: Jobs that exceeded max retries

#### 4. List Jobs

List all jobs or filter by state:

```bash
# List all jobs
queuectl list

# List jobs by state
queuectl list --state PENDING
queuectl list --state COMPLETED
queuectl list --state DEAD
```

#### 5. Dead Letter Queue (DLQ)

##### List DLQ Jobs
```bash
queuectl dlq-list
```

##### Retry a Job from DLQ
```bash
queuectl dlq-retry <job-id>
```

This resets the job's attempt count and moves it back to PENDING state.

#### 6. Configuration

Set configuration values:

```bash
queuectl config <key> <value>
```

Examples:
```bash
queuectl config max-retries 5
queuectl config poll-interval-ms 1000
queuectl config base-backoff-ms 2000
```

## Architecture

### Components

1. **Job Model** (`Job.java`)
   - Represents a job with fields: id, command, state, attempts, maxRetries, timestamps
   - Job states: PENDING, PROCESSING, COMPLETED, FAILED, DEAD

2. **Job Repository** (`JobRepository.java`)
   - Thread-safe file-based persistence using `ReadWriteLock`
   - Stores jobs in `jobs.json`
   - Provides CRUD operations and queries

3. **Job Executor** (`JobExecutor.java`)
   - Executes shell commands using `ProcessBuilder`
   - Cross-platform support (Windows/Unix)
   - Returns exit codes for success/failure detection

4. **Worker Service** (`WorkerService.java`)
   - Manages pool of worker threads using `ExecutorService`
   - Implements job processing loop with retry logic
   - Exponential backoff: `delay = base * 2^(attempts-1)`

5. **CLI Commands** (`commands` package)
   - Picocli-based command-line interface
   - Individual command classes for each operation

6. **Configuration Service** (`ConfigurationService.java`)
   - Manages runtime configuration in `config.json`
   - Supports get/set operations for configuration values

### Data Flow

```
┌─────────────┐
│   Enqueue   │ → Job created with PENDING state
└─────────────┘
       ↓
┌─────────────┐
│ jobs.json   │ ← Persistent storage
└─────────────┘
       ↓
┌─────────────┐
│   Worker    │ → Picks PENDING job, sets to PROCESSING
└─────────────┘
       ↓
┌─────────────┐
│  Execute    │ → Runs command, returns exit code
└─────────────┘
       ↓
    Success? ──Yes──→ COMPLETED
       │
       No
       ↓
 Attempts < Max? ──Yes──→ FAILED (retry with backoff) → PENDING
       │
       No
       ↓
    DEAD (DLQ)
```

### Thread Safety

The system uses a `ReentrantReadWriteLock` in `JobRepository` to ensure thread-safe access to `jobs.json`:
- **Write Lock**: Used for add, update operations
- **Read Lock**: Used for query operations
- Multiple workers can safely process jobs concurrently

### Retry Strategy

Failed jobs are retried with exponential backoff:
- Base delay: 1000ms (configurable)
- Backoff formula: `delay = base × 2^(attempts-1)`
- Example: 1s → 2s → 4s → 8s
- After max retries, job moves to DEAD state

## Testing

Run the validation script to test core functionality:

```bash
bash validate.sh
```

The script tests:
1. Job enqueuing (successful and failing jobs)
2. Job listing and filtering
3. Worker processing
4. Status reporting
5. Dead letter queue
6. DLQ retry mechanism
7. Configuration management

## File Structure

```
flam-backend-assignment/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── hars/
│                   └── queuectl/
│                       ├── Main.java
│                       ├── commands/
│                       │   ├── QueueCtlCommand.java
│                       │   ├── EnqueueCommand.java
│                       │   ├── WorkerStartCommand.java
│                       │   ├── StatusCommand.java
│                       │   ├── ListCommand.java
│                       │   ├── DLQListCommand.java
│                       │   ├── DLQRetryCommand.java
│                       │   └── ConfigSetCommand.java
│                       ├── model/
│                       │   └── Job.java
│                       ├── service/
│                       │   ├── JobRepository.java
│                       │   ├── WorkerService.java
│                       │   └── ConfigurationService.java
│                       └── worker/
│                           └── JobExecutor.java
├── pom.xml
├── README.md
├── validate.sh
├── jobs.json (created at runtime)
└── config.json (created at runtime)
```

## Dependencies

- **Picocli 4.7.5**: Command-line interface framework
- **Jackson 2.15.2**: JSON serialization/deserialization
- **Jackson JSR310**: Java 8 date/time support

## Configuration Options

The following configuration keys are supported:

- `max-retries`: Maximum retry attempts for failed jobs (default: 3)
- `poll-interval-ms`: Worker polling interval when no jobs available (default: 500)
- `base-backoff-ms`: Base delay for exponential backoff (default: 1000)

## Troubleshooting

### Workers not processing jobs
- Check if workers are running: `queuectl status`
- Verify jobs are in PENDING state: `queuectl list --state PENDING`
- Check `jobs.json` file exists and is readable

### Job execution fails
- Verify the command syntax is correct for your shell
- Check file permissions for scripts
- Review worker output for error messages

### Jobs stuck in PROCESSING
- Stop workers (Ctrl+C) and restart them
- Check if a worker process crashed
- Jobs should reset to PENDING or FAILED on worker restart

## Future Enhancements

Possible improvements:
- Job priorities
- Scheduled/delayed jobs
- Job dependencies
- Web dashboard
- Distributed workers
- Job output capture
- Webhook notifications

## License

This project is part of the FLAM Backend Assignment.

## Author

Created as part of the FLAM Backend Assignment.
