package core;

import org.hibernate.internal.util.collections.ConcurrentReferenceHashMap;
import org.junit.runner.Computer;
import org.junit.runner.Runner;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.RunnerScheduler;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class ParallelComputer extends Computer implements AutoCloseable {
    private static ConcurrentReferenceHashMap<ParallelComputer, Thread> computers = new ConcurrentReferenceHashMap<>();

    private java.lang.Class<?>[] classes;
    private ExecutorService executorService;
    private SchedulerListener schedulerListener;
    private ConcurrentHashMap<Thread, BlockingQueue> context = new ConcurrentHashMap<>();

    public ParallelComputer(java.lang.Class<?>[] classes, SchedulerListener schedulerListener) {
        this.classes = classes;
        computers.put(this, Thread.currentThread());
        context.put(Thread.currentThread(), new ArrayBlockingQueue(100));
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, Thread.currentThread().getName() + "-" + r.getClass().getName() + "-" + System.nanoTime());
            context.put(thread, new ArrayBlockingQueue(100));
            return thread;
        });
        this.schedulerListener = schedulerListener;
    }

    public static void send(Object data) {
        ParallelComputer computer = getComputerByThread();
        if (computer != null) {
            for (Map.Entry<Thread, BlockingQueue> queueMap : computer.context.entrySet()) {
                if (queueMap.getKey() != Thread.currentThread()) {
                    queueMap.getValue().add(data);
                }
            }
        }

    }

    public static BlockingQueue getQueue() {
        ParallelComputer computer = getComputerByThread();
        return computer != null ? computer.context.get(Thread.currentThread()) : null;
    }

    private static ParallelComputer getComputerByThread() {
        for (ParallelComputer computer : computers.keySet()) {
            if (computer.context.containsKey(Thread.currentThread())) {
                return computer;
            }
        }
        return null;
    }

    public void close() throws Exception {
        computers.remove(this);
    }

    private Runner parallelize(Runner runner) {
        if (runner instanceof ParentRunner) {
            ((ParentRunner<?>) runner).setScheduler(new RunnerScheduler() {

                public void schedule(Runnable childStatement) {
                    executorService.submit(childStatement);
                }

                public void finished() {
                    if (schedulerListener != null && schedulerListener.finished(executorService, Collections.unmodifiableSet(context.keySet()))) {
                        try {
                            executorService.shutdown();
                            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace(System.err);
                        }
                    }
                }
            });
        }
        return runner;
    }

    public Class<?>[] getClasses() {
        return classes;
    }

    @Override
    public Runner getSuite(RunnerBuilder builder, java.lang.Class<?>[] classes)
            throws InitializationError {
        Runner suite = super.getSuite(builder, classes);
        return parallelize(suite);
    }

    @Override
    protected Runner getRunner(RunnerBuilder builder, Class<?> testClass)
            throws Throwable {
        return super.getRunner(builder, testClass);
    }

    public interface SchedulerListener {
        boolean finished(ExecutorService executorService, Set<Thread> threads);
    }
}
