#!/bin/bash

# Validation script for queuectl
# Tests core functionality of the job queue system

echo "========================================"
echo "queuectl Validation Script"
echo "========================================"
echo ""

# Clean up any existing data
echo "Step 1: Cleaning up existing data..."
rm -f jobs.json config.json
echo "✓ Cleanup complete"
echo ""

# Build the project
echo "Step 2: Building the project..."
mvn clean package -q
if [ $? -ne 0 ]; then
    echo "✗ Build failed"
    exit 1
fi
echo "✓ Build successful"
echo ""

# Set up alias for easier command execution
QUEUECTL="java -jar target/flam-backend-assignment-1.0-SNAPSHOT.jar"

# Test 1: Enqueue successful job
echo "Step 3: Enqueuing a successful job..."
$QUEUECTL enqueue '{"command":"echo Hello World","max_retries":3}'
if [ $? -ne 0 ]; then
    echo "✗ Failed to enqueue successful job"
    exit 1
fi
echo "✓ Successful job enqueued"
echo ""

# Test 2: Enqueue failing job
echo "Step 4: Enqueuing a failing job..."
$QUEUECTL enqueue '{"command":"exit 1","max_retries":2}'
if [ $? -ne 0 ]; then
    echo "✗ Failed to enqueue failing job"
    exit 1
fi
echo "✓ Failing job enqueued"
echo ""

# Test 3: List pending jobs
echo "Step 5: Listing pending jobs..."
PENDING_COUNT=$($QUEUECTL list --state PENDING | grep -c '"id"')
if [ "$PENDING_COUNT" -lt 2 ]; then
    echo "✗ Expected at least 2 pending jobs, found $PENDING_COUNT"
    exit 1
fi
echo "✓ Found $PENDING_COUNT pending jobs"
echo ""

# Test 4: Check initial status
echo "Step 6: Checking initial queue status..."
$QUEUECTL status
echo ""

# Test 5: Start workers in background
echo "Step 7: Starting 2 workers..."
$QUEUECTL worker --count 2 &
WORKER_PID=$!
echo "✓ Workers started with PID $WORKER_PID"
echo ""

# Test 6: Wait and check for job processing
echo "Step 8: Waiting for jobs to be processed (30 seconds)..."
sleep 30
echo ""

# Test 7: Check final status
echo "Step 9: Checking final queue status..."
$QUEUECTL status
echo ""

# Test 8: Verify successful job completed
echo "Step 10: Verifying successful job completion..."
COMPLETED_COUNT=$($QUEUECTL list --state COMPLETED | grep -c '"id"')
if [ "$COMPLETED_COUNT" -lt 1 ]; then
    echo "✗ Expected at least 1 completed job, found $COMPLETED_COUNT"
    kill $WORKER_PID 2>/dev/null
    exit 1
fi
echo "✓ Found $COMPLETED_COUNT completed job(s)"
echo ""

# Test 9: Verify failing job in DLQ
echo "Step 11: Checking dead letter queue..."
DLQ_COUNT=$($QUEUECTL dlq-list | grep -c '"id"')
if [ "$DLQ_COUNT" -lt 1 ]; then
    echo "✗ Expected at least 1 job in DLQ, found $DLQ_COUNT"
    kill $WORKER_PID 2>/dev/null
    exit 1
fi
echo "✓ Found $DLQ_COUNT job(s) in DLQ"
echo ""

# Test 10: Get DLQ job ID and retry it
echo "Step 12: Retrying a job from DLQ..."
DLQ_JOB_ID=$($QUEUECTL dlq-list | grep -o '"id" : "[^"]*"' | head -1 | cut -d'"' -f4)
if [ -n "$DLQ_JOB_ID" ]; then
    $QUEUECTL dlq-retry "$DLQ_JOB_ID"
    if [ $? -ne 0 ]; then
        echo "✗ Failed to retry job from DLQ"
        kill $WORKER_PID 2>/dev/null
        exit 1
    fi
    echo "✓ Job $DLQ_JOB_ID retried successfully"
else
    echo "✗ Could not find job ID in DLQ"
    kill $WORKER_PID 2>/dev/null
    exit 1
fi
echo ""

# Test 11: Verify job is back in PENDING
echo "Step 13: Verifying retried job is back in PENDING state..."
sleep 2
PENDING_AFTER_RETRY=$($QUEUECTL list --state PENDING | grep -c "$DLQ_JOB_ID")
if [ "$PENDING_AFTER_RETRY" -lt 1 ]; then
    echo "⚠ Warning: Retried job may not be in PENDING state (it might have been processed already)"
else
    echo "✓ Retried job is back in PENDING state"
fi
echo ""

# Test 12: Test configuration
echo "Step 14: Testing configuration..."
$QUEUECTL config max-retries 5
if [ $? -ne 0 ]; then
    echo "✗ Failed to set configuration"
    kill $WORKER_PID 2>/dev/null
    exit 1
fi
echo "✓ Configuration updated successfully"
echo ""

# Clean up
echo "Step 15: Stopping workers..."
kill $WORKER_PID 2>/dev/null
wait $WORKER_PID 2>/dev/null
echo "✓ Workers stopped"
echo ""

echo "========================================"
echo "✓ All validation tests passed!"
echo "========================================"
exit 0
