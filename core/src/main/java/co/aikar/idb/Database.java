package co.aikar.idb;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface Database {

    /**
     * Called in onDisable, destroys the Data source and nulls out references.
     */
    default void close() {
        close(120, TimeUnit.SECONDS);
    }

    /**
     * Called in onDisable, destroys the Data source and nulls out references.
     */
    void close(long timeout, @NotNull TimeUnit unit);

    <T> CompletableFuture<T> dispatchAsync(@NotNull Callable<T> task);

    /**
     * Get a JDBC Connection
     */
    @Nullable
    Connection getConnection() throws SQLException;

    /**
     * Create a Timings object
     */
    DatabaseTiming timings(@NotNull String name);

    /**
     * Get the Logger
     */
    Logger getLogger();

    /**
     * Get the options object
     */
    DatabaseOptions getOptions();

    default void fatalError(@NotNull Exception e) {
        getOptions().onFatalError.accept(e);
    }

    default void closeConnection(@NotNull Connection conn) throws SQLException {
        conn.close();
    }

    /**
     * Initiates a new DbStatement
     * <p/>
     * YOU MUST MANUALLY CLOSE THIS STATEMENT IN A finally {} BLOCK!
     */
    default DbStatement createStatement() throws SQLException {
        return (new DbStatement(this));
    }

    /**
     * Initiates a new DbStatement and prepares the first query.
     * <p/>
     * YOU MUST MANUALLY CLOSE THIS STATEMENT IN A finally {} BLOCK!
     */
    default DbStatement query(@Language("SQL") @NotNull String query) throws SQLException {
        DbStatement stm = new DbStatement(this);
        try {
            stm.query(query);
            return stm;
        } catch (Exception e) {
            stm.close();
            throw e;
        }
    }

    /**
     * Initiates a new DbStatement and prepares the first query.
     * <p/>
     * YOU MUST MANUALLY CLOSE THIS STATEMENT IN A finally {} BLOCK!
     */
    default CompletableFuture<DbStatement> queryAsync(@Language("SQL") @NotNull String query) {
        return dispatchAsync(() -> new DbStatement(this).query(query));
    }

    /**
     * Utility method to execute a query and retrieve the first row, then close statement.
     * You should ensure result will only return 1 row for maximum performance.
     *
     * @param query  The query to run
     * @param params The parameters to execute the statement with
     * @return DbRow of your results (HashMap with template return type)
     */
    default DbRow getFirstRow(@Language("SQL") @NotNull String query, @NotNull Object... params) throws SQLException {
        try (DbStatement statement = query(query)) {
            statement.execute(params);
            return statement.getNextRow();
        }
    }

    /**
     * Utility method to execute a query and retrieve the first row, then close statement.
     * You should ensure result will only return 1 row for maximum performance.
     *
     * @param query  The query to run
     * @param params The parameters to execute the statement with
     * @return DbRow of your results (HashMap with template return type)
     */
    default CompletableFuture<DbRow> getFirstRowAsync(@Language("SQL") @NotNull String query, @NotNull Object... params) {
        return dispatchAsync(() -> getFirstRow(query, params));
    }

    /**
     * Utility method to execute a query and retrieve the first column of the first row, then close statement.
     * You should ensure result will only return 1 row for maximum performance.
     *
     * @param query  The query to run
     * @param params The parameters to execute the statement with
     * @return DbRow of your results (HashMap with template return type)
     */
    default <T> T getFirstColumn(@Language("SQL") @NotNull String query, @NotNull Object... params) throws SQLException {
        try (DbStatement statement = query(query)) {
            statement.execute(params);
            return statement.getFirstColumn();
        }
    }

    /**
     * Utility method to execute a query and retrieve the first column of the first row, then close statement.
     * You should ensure result will only return 1 row for maximum performance.
     *
     * @param query  The query to run
     * @param params The parameters to execute the statement with
     * @return DbRow of your results (HashMap with template return type)
     */
    default <T> CompletableFuture<T> getFirstColumnAsync(@Language("SQL") @NotNull String query, @NotNull Object... params) {
        return dispatchAsync(() -> getFirstColumn(query, params));
    }

    /**
     * Utility method to execute a query and retrieve first column of all results, then close statement.
     * <p>
     * Meant for single queries that will not use the statement multiple times.
     */
    default <T> List<T> getFirstColumnResults(@Language("SQL") @NotNull String query, @NotNull Object... params) throws SQLException {
        List<T> dbRows = new ArrayList<>();
        T result;
        try (DbStatement statement = query(query)) {
            statement.execute(params);
            while ((result = statement.getFirstColumn()) != null) {
                dbRows.add(result);
            }
        }
        return dbRows;
    }

    /**
     * Utility method to execute a query and retrieve first column of all results, then close statement.
     * <p>
     * Meant for single queries that will not use the statement multiple times.
     */
    default <T> CompletableFuture<List<T>> getFirstColumnResultsAsync(@Language("SQL") @NotNull String query, @NotNull Object... params) {
        return dispatchAsync(() -> getFirstColumnResults(query, params));
    }

    /**
     * Utility method to execute a query and retrieve all results, then close statement.
     * <p>
     * Meant for single queries that will not use the statement multiple times.
     *
     * @param query  The query to run
     * @param params The parameters to execute the statement with
     * @return List of DbRow of your results (HashMap with template return type)
     */
    default List<DbRow> getResults(@Language("SQL") @NotNull String query, @NotNull Object... params) throws SQLException {
        try (DbStatement statement = query(query)) {
            statement.execute(params);
            return statement.getResults();
        }
    }

    /**
     * Utility method to execute a query and retrieve all results, then close statement.
     * <p>
     * Meant for single queries that will not use the statement multiple times.
     *
     * @param query  The query to run
     * @param params The parameters to execute the statement with
     * @return List of DbRow of your results (HashMap with template return type)
     */
    default CompletableFuture<List<DbRow>> getResultsAsync(@Language("SQL") @NotNull String query, @NotNull Object... params) {
        return dispatchAsync(() -> getResults(query, params));
    }

    /**
     * Utility method for executing an update synchronously that does an insert,
     * closes the statement, and returns the last insert ID.
     *
     * @param query  Query to run
     * @param params Params to execute the statement with.
     * @return Inserted Row Id.
     */
    @Nullable
    default Long executeInsert(@Language("SQL") @NotNull String query, @NotNull Object... params) throws SQLException {
        try (DbStatement statement = query(query)) {
            int i = statement.executeUpdate(params);
            if (i > 0) {
                return statement.getLastInsertId();
            }
        }
        return null;
    }

    /**
     * Utility method for executing an update synchronously, and then close the statement.
     *
     * @param query  Query to run
     * @param params Params to execute the statement with.
     * @return Number of rows modified.
     */
    default int executeUpdate(@Language("SQL") @NotNull String query, @NotNull Object... params) throws SQLException {
        try (DbStatement statement = query(query)) {
            return statement.executeUpdate(params);
        }
    }

    /**
     * Utility method to execute an update statement asynchronously and close the connection.
     *
     * @param query  Query to run
     * @param params Params to execute the update with
     */
    default CompletableFuture<Integer> executeUpdateAsync(@Language("SQL") @NotNull String query, @NotNull final Object... params) {
        return dispatchAsync(() -> executeUpdate(query, params));
    }

    @Nullable
    default void createTransactionAsync(@NotNull TransactionCallback run) {
        createTransactionAsync(run, null, null);
    }

    default void createTransactionAsync(@NotNull TransactionCallback run, Runnable onSuccess, Runnable onFail) {
        dispatchAsync(() -> {
            if (!createTransaction(run)) {
                if (onFail != null) {
                    onFail.run();
                }
            } else if (onSuccess != null) {
                onSuccess.run();
            }
            return null;
        });
    }

    default boolean createTransaction(@NotNull TransactionCallback run) {
        try (DbStatement stm = new DbStatement(this)) {
            try {
                stm.startTransaction();
                if (!run.apply(stm)) {
                    stm.rollback();
                    return false;
                } else {
                    stm.commit();
                    return true;
                }
            } catch (Exception e) {
                stm.rollback();
                DB.logException(e);
            }
        } catch (SQLException e) {
            DB.logException(e);
        }
        return false;
    }

    default void logException(@NotNull String message, @NotNull Exception e) {
        DB.logException(getLogger(), Level.SEVERE, message, e);
    }

    default void logException(@NotNull Exception e) {
        DB.logException(getLogger(), Level.SEVERE, e.getMessage(), e);
    }
}
