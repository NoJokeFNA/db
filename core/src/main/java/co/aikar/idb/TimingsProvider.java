package co.aikar.idb;

import org.jetbrains.annotations.NotNull;

public interface TimingsProvider {

    default DatabaseTiming ofStart(@NotNull String name) {
        return this.ofStart(name, null);
    }

    default DatabaseTiming ofStart(@NotNull String name, DatabaseTiming parent) {
        return this.of(name, parent).startTiming();
    }

    default DatabaseTiming of(@NotNull String name) {
        return this.of(name, null);
    }

    DatabaseTiming of(@NotNull String name, DatabaseTiming parent);
}
