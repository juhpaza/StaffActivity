package fi.juhpaza.staffactivity.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Owns the StaffActivity SQLite connection and keeps database work off the server thread.
 */
public final class DatabaseService {
    private static final Duration SHUTDOWN_WAIT = Duration.ofSeconds(10);

    private final JavaPlugin plugin;
    private final DatabaseMigrator migrator;
    private final ExecutorService executor;
    private final AtomicInteger pendingOperations = new AtomicInteger();

    private volatile DatabaseStatus status = DatabaseStatus.NOT_STARTED;
    private Connection connection;

    public DatabaseService(JavaPlugin plugin) {
        this(plugin, new DatabaseMigrator());
    }

    DatabaseService(JavaPlugin plugin, DatabaseMigrator migrator) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.migrator = Objects.requireNonNull(migrator, "migrator");
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "StaffActivity-Database");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<Void> initialize() {
        status = DatabaseStatus.INITIALIZING;
        return runAsync(() -> {
            try {
                Class.forName("org.sqlite.JDBC");
                plugin.getDataFolder().mkdirs();
                String url = "jdbc:sqlite:" + plugin.getDataFolder().toPath().resolve("staffactivity.db").toAbsolutePath();
                connection = DriverManager.getConnection(url);
                migrator.migrate(connection);
                status = DatabaseStatus.READY;
            } catch (ClassNotFoundException | SQLException ex) {
                status = DatabaseStatus.FAILED;
                throw new IllegalStateException("Could not initialize SQLite database", ex);
            }
        });
    }

    public DatabaseStatus status() {
        return status;
    }

    public int pendingOperations() {
        return pendingOperations.get();
    }

    public void close() {
        runAsync(() -> {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    plugin.getLogger().warning("Failed to close database connection cleanly: " + ex.getMessage());
                }
            }
            status = DatabaseStatus.CLOSED;
        });
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_WAIT.toSeconds(), TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Database executor did not stop within " + SHUTDOWN_WAIT.toSeconds() + " seconds.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private CompletableFuture<Void> runAsync(CheckedRunnable runnable) {
        pendingOperations.incrementAndGet();
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                pendingOperations.decrementAndGet();
            }
        }, executor);
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
