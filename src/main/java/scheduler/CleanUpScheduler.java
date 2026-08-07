package scheduler;
import java.util.concurrent.*;
/**
 * A singleton scheduler responsible for executing periodic background
 * cleanup tasks shared across all cache instances.
 *
 * <p>Instead of creating a dedicated scheduler thread for every
 * {@code InMemoryCache}, this class maintains a shared
 * {@link ScheduledExecutorService} that schedules cleanup tasks for
 * multiple caches. This significantly reduces thread creation overhead
 * when many cache instances are created.
 *
 * <p>Each registered cleanup task is scheduled with a fixed delay and
 * executes independently. A cache instance receives a
 * {@link ScheduledFuture} when registering its cleanup task, allowing
 * it to cancel only its own task without affecting cleanup tasks
 * belonging to other caches.
 *
 * <p>This class is thread-safe. The underlying
 * {@link ScheduledExecutorService} safely supports concurrent task
 * registration from multiple threads.
 */
public class CleanUpScheduler {
    /**
     * Singleton instance of the cleanup scheduler.
     */
    private static final CleanUpScheduler cleanUpScheduler=new CleanUpScheduler();

    /**
     * Shared executor responsible for running all registered
     * background cleanup tasks.
     */
    private final ScheduledExecutorService executorService= Executors.newScheduledThreadPool(20);

    /**
     * Prevents external instantiation.
     */
    private CleanUpScheduler() {}

    /**
     * Returns the singleton cleanup scheduler.
     *
     * @return the shared {@code CleanUpScheduler} instance
     */
    public static CleanUpScheduler getInstance(){
        return cleanUpScheduler;
    }

    /**
     * Registers a periodic cleanup task with the shared scheduler.
     *
     * <p>The supplied task is executed repeatedly with a fixed delay.
     * The returned {@link ScheduledFuture} can be used to cancel
     * subsequent executions of this specific task without affecting
     * other scheduled cleanup tasks.
     *
     * @param task the cleanup task to execute periodically
     * @param cleanUpIntervalMillis the delay, in milliseconds, between
     *                              the completion of one execution and
     *                              the commencement of the next
     * @return a {@link ScheduledFuture} representing the scheduled task
     */
    public <K,V> ScheduledFuture<?> scheduleForCleanUp(Runnable task, long cleanUpIntervalMillis){
       return executorService.scheduleWithFixedDelay(task,cleanUpIntervalMillis,cleanUpIntervalMillis, TimeUnit.MILLISECONDS);
    }


    /**
     * Gracefully shuts down the shared scheduler.
     *
     * <p>No new cleanup tasks are accepted after shutdown. Existing
     * running tasks are allowed to complete. If the scheduler does not
     * terminate within the timeout period, all remaining tasks are
     * cancelled by invoking {@link ScheduledExecutorService#shutdownNow()}.
     *
     * <p>This method is intended to be called during application
     * shutdown.
     */
    public void shutdown(){
        executorService.shutdown();
        try {
            if(!executorService.awaitTermination(10,TimeUnit.SECONDS)){
                executorService.shutdownNow();
            }
        }catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
