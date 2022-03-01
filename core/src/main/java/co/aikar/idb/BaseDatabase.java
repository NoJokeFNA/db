package co.aikar.idb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BaseDatabase implements Database {
    private TimingsProvider timingsProvider;
    private DatabaseTiming sqlTiming;
    private Logger logger;
    private DatabaseOptions options;
    private ExecutorService threadPool;
    DataSource dataSource;

    public BaseDatabase(@NotNull DatabaseOptions options) {
        this.options = options;
        if (options.driverClassName != null && !options.favorDataSourceOverDriver) {
            options.dataSourceClassName = null;
        }
        if (options.driverClassName == null && options.dataSourceClassName == null) {
            throw new NullPointerException("Both driverClassName and dataSourceClassName can not be null. Please load an appropriate DataSource or Driver.");
        }
        this.timingsProvider = options.timingsProvider;
        this.threadPool = options.executor;
        if (this.threadPool == null) {
            this.threadPool = new ThreadPoolExecutor(
                    options.minAsyncThreads,
                    options.maxAsyncThreads,
                    options.asyncThreadTimeout,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>()
            );
            ((ThreadPoolExecutor) threadPool).allowCoreThreadTimeOut(true);
        }
        this.sqlTiming = timingsProvider.of("Database");
        this.logger = options.logger;
        if (this.logger == null) {
            this.logger = Logger.getLogger(options.poolName);
        }
        this.logger.info("Connecting to Database: " + options.dsn);
    }

    public void close(long timeout, @NotNull TimeUnit unit) {
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            logException(e);
        }
        if (dataSource instanceof Closeable) {
            try {
                ((Closeable) dataSource).close();
            } catch (IOException e) {
                logException(e);
            } finally {
                dataSource = null;
            }
        }
    }


    @Override
    public synchronized <T> CompletableFuture<T> dispatchAsync(@NotNull Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable run = () -> {
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        };
        if (threadPool == null) {
            run.run();
        } else {
            threadPool.submit(run);
        }
        return future;
    }

    @Override
    public DatabaseTiming timings(@NotNull String name) {
        return timingsProvider.of(options.poolName + " - " + name, sqlTiming);
    }

    @Override
    public DatabaseOptions getOptions() {
        return this.options;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public @Nullable Connection getConnection() throws SQLException {
        return dataSource != null ? dataSource.getConnection() : null;
    }
}
