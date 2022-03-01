package co.aikar.idb;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("UnusedAssignment")
@Builder(toBuilder = true) @Data
public class PooledDatabaseOptions {
    @Builder.Default int minIdleConnections = 3;
    @Builder.Default int maxConnections = 5;
    Map<String, Object> dataSourceProperties;
    @NotNull DatabaseOptions options;

    public static class PooledDatabaseOptionsBuilder  {
        public HikariPooledDatabase createHikariDatabase() {
            return new HikariPooledDatabase(this.build());
        }
    }
}
