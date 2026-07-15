package fi.juhpaza.staffactivity.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
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
import fi.juhpaza.staffactivity.model.RecentTeleport;
import fi.juhpaza.staffactivity.model.StaffReportEntry;
import fi.juhpaza.staffactivity.model.StaffSummary;
import fi.juhpaza.staffactivity.model.TeleportRecord;
import fi.juhpaza.staffactivity.model.TopEntry;
import fi.juhpaza.staffactivity.repository.StaffSessionRepository;
import fi.juhpaza.staffactivity.repository.StaffStatsRepository;
import fi.juhpaza.staffactivity.repository.StaffTeleportRepository;
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
    private final StaffTeleportRepository teleportRepository;
    private final ExecutorService executor;
    private final AtomicInteger pendingOperations = new AtomicInteger();

    private volatile DatabaseStatus status = DatabaseStatus.NOT_STARTED;
    private Connection connection;

    public DatabaseService(JavaPlugin plugin) {
        this(plugin, new DatabaseMigrator(), new StaffSessionRepository(), new StaffStatsRepository(), new StaffTeleportRepository());
    }

    DatabaseService(JavaPlugin plugin, DatabaseMigrator migrator, StaffSessionRepository sessionRepository, StaffStatsRepository statsRepository, StaffTeleportRepository teleportRepository) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.migrator = Objects.requireNonNull(migrator, "migrator");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository");
        this.statsRepository = Objects.requireNonNull(statsRepository, "statsRepository");
        this.teleportRepository = Objects.requireNonNull(teleportRepository, "teleportRepository");
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "StaffActivity-Database");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<Void> initialize(ZoneId timezone) {
        status = DatabaseStatus.INITIALIZING;
        return runAsync(() -> {
            try {
                Class.forName("org.sqlite.JDBC");
                plugin.getDataFolder().mkdirs();
                String url = "jdbc:sqlite:" + plugin.getDataFolder().toPath().resolve("staffactivity.db").toAbsolutePath();
                connection = DriverManager.getConnection(url);
                migrator.migrate(connection);
                recoverActiveSessions(timezone);
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

    public CompletableFuture<Void> saveActiveSessions(List<SessionSnapshot> snapshots) {
        return runAsync(() -> {
            ensureReady();
            sessionRepository.saveActiveSessions(connection, snapshots);
        });
    }

    public CompletableFuture<Void> saveTeleport(TeleportRecord record) {
        return runAsync(() -> {
            ensureReady();
            teleportRepository.saveTeleport(connection, record);
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

    public CompletableFuture<List<RecentTeleport>> findRecentTeleports(String uuid, int limit) {
        return supplyAsync(() -> {
            ensureReady();
            return teleportRepository.findRecentTeleports(connection, uuid, limit);
        });
    }

    public CompletableFuture<Integer> countTeleports(String uuid) {
        return supplyAsync(() -> {
            ensureReady();
            return teleportRepository.countTeleports(connection, uuid);
        });
    }

    public CompletableFuture<List<String>> findMissingTables(List<String> tableNames) {
        return supplyAsync(() -> {
            ensureReady();
            List<String> missingTables = new ArrayList<>();
            for (String tableName : tableNames) {
                if (!tableExists(tableName)) {
                    missingTables.add(tableName);
                }
            }
            return List.copyOf(missingTables);
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

    public CompletableFuture<List<StaffReportEntry>> findDailyReport(String statDate) {
        return supplyAsync(() -> {
            ensureReady();
            return statsRepository.findDailyReport(connection, statDate);
        });
    }

    public CompletableFuture<List<StaffReportEntry>> findPeriodReport(String startDateInclusive, String endDateInclusive) {
        return supplyAsync(() -> {
            ensureReady();
            return statsRepository.findPeriodReport(connection, startDateInclusive, endDateInclusive);
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

    private boolean tableExists(String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT name
                FROM sqlite_master
                WHERE type = 'table' AND name = ?
                """)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void recoverActiveSessions(ZoneId timezone) throws SQLException {
        List<SessionSnapshot> recovered = sessionRepository.loadActiveSessionsForRecovery(connection);
        for (SessionSnapshot snapshot : recovered) {
            sessionRepository.saveClosedSession(connection, snapshot, timezone);
        }
        if (!recovered.isEmpty()) {
            plugin.getLogger().warning("Recovered " + recovered.size() + " active staff session(s) from previous shutdown.");
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
