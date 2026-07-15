package fi.juhpaza.staffactivity.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import fi.juhpaza.staffactivity.model.SessionSnapshot;
import fi.juhpaza.staffactivity.model.DailyStats;
import fi.juhpaza.staffactivity.model.PeriodStats;
import fi.juhpaza.staffactivity.model.RecentSession;
import fi.juhpaza.staffactivity.model.StaffSummary;
import fi.juhpaza.staffactivity.model.TopEntry;
import fi.juhpaza.staffactivity.repository.StaffSessionRepository;
import fi.juhpaza.staffactivity.repository.StaffStatsRepository;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Owns the StaffActivity SQLite connection and keeps database work off the server thread.
 */
public final class DatabaseService {
    private static final Duration SHUTDOWN_WAIT = Duration.ofSeconds(10);

    private final JavaPlugin plugin;
    private final DatabaseMigrator migrator;
    private final StaffSessionRepository sessionRepository;
    private final StaffStatsRepository statsRepository;
    private final ExecutorService executor;
    private final AtomicInteger pendingOperations = new AtomicInteger();

    private volatile DatabaseStatus status = DatabaseStatus.NOT_STARTED;
    private Connection connection;

    public DatabaseService(JavaPlugin plugin) {
        this(plugin, new DatabaseMigrator(), new StaffSessionRepository(), new StaffStatsRepository());
    }

    DatabaseService(JavaPlugin plugin, DatabaseMigrator migrator, StaffSessionRepository sessionRepository, StaffStatsRepository statsRepository) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.migrator = Objects.requireNonNull(migrator, "migrator");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository");
        this.statsRepository = Objects.requireNonNull(statsRepository, "statsRepository");
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

    public CompletableFuture<Void> saveClosedSession(SessionSnapshot snapshot, ZoneId timezone) {
        return runAsync(() -> {
            if (status != DatabaseStatus.READY || connection == null) {
                throw new IllegalStateException("Database is not ready");
            }
            sessionRepository.saveClosedSession(connection, snapshot, timezone);
        });
    }

    public CompletableFuture<java.util.Optional<StaffSummary>> findSummaryByUuid(java.util.UUID uuid) {
        return supplyAsync(() -> {
            ensureReady();
            return statsRepository.findSummaryByUuid(connection, uuid);
        });
    }

    public CompletableFuture<java.util.Optional<StaffSummary>> findSummaryByName(String latestName) {
        return supplyAsync(() -> {
            ensureReady();
            return statsRepository.findSummaryByName(connection, latestName);
        });
    }

    public CompletableFuture<java.util.Optional<DailyStats>> findDailyStats(String uuid, String statDate) {
        return supplyAsync(() -> {
            ensureReady();
            return statsRepository.findDailyStats(connection, uuid, statDate);
        });
    }

    public CompletableFuture<java.util.List<RecentSession>> findRecentSessions(String uuid, int limit) {
        return supplyAsync(() -> {
            ensureReady();
            return statsRepository.findRecentSessions(connection, uuid, limit);
        });
    }

    public CompletableFuture<PeriodStats> findPeriodStats(String uuid, String startDateInclusive, String endDateInclusive) {
        return supplyAsync(() -> {
            ensureReady();
            return statsRepository.findPeriodStats(connection, uuid, startDateInclusive, endDateInclusive);
        });
    }

    public CompletableFuture<java.util.List<TopEntry>> findTop(String metricColumn, int limit) {
        return supplyAsync(() -> {
            ensureReady();
            return statsRepository.findTop(connection, metricColumn, limit);
        });
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

    private <T> CompletableFuture<T> supplyAsync(CheckedSupplier<T> supplier) {
        pendingOperations.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                pendingOperations.decrementAndGet();
            }
        }, executor);
    }

    private void ensureReady() {
        if (status != DatabaseStatus.READY || connection == null) {
            throw new IllegalStateException("Database is not ready");
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
