package co.aikar.idb;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.function.Function;

public interface TransactionCallback extends Function<DbStatement, Boolean> {
    @Override @SneakyThrows
    default Boolean apply(@NotNull DbStatement dbStatement) {
        return this.runTransaction(dbStatement);
    }

    Boolean runTransaction(@NotNull DbStatement stm) throws SQLException;
}
