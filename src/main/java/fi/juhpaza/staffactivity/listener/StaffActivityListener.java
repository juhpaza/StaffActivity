package fi.juhpaza.staffactivity.listener;

import fi.juhpaza.staffactivity.StaffActivity;
import fi.juhpaza.staffactivity.command.CommandRootExtractor;
import fi.juhpaza.staffactivity.config.ConfigService;
import fi.juhpaza.staffactivity.model.SessionCloseReason;
import fi.juhpaza.staffactivity.model.TeleportRecord;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.time.Instant;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Translates Paper player events into StaffActivity session updates.
 */
public final class StaffActivityListener implements Listener {
    private final StaffActivity plugin;

    public StaffActivityListener(StaffActivity plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (ensureTrackingState(event.getPlayer(), Instant.now())) {
            plugin.discordReportService().staffJoin(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        boolean wasTracked = plugin.sessionService().session(event.getPlayer().getUniqueId()).isPresent();
        plugin.sessionService().closeSession(event.getPlayer().getUniqueId(), Instant.now(), SessionCloseReason.NORMAL)
                .ifPresent(plugin::persistClosedSession);
        if (wasTracked) {
            plugin.discordReportService().staffQuit(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        ConfigService config = plugin.configService();
        if (!config.movementTrigger() && !config.rotationTrigger()) {
            return;
        }
        Player player = event.getPlayer();
        Instant now = Instant.now();
        if (!ensureTrackingState(player, now)) {
            return;
        }
        if (isMeaningfulMovement(event.getFrom(), event.getTo(), config)) {
            plugin.sessionService().markActivity(player.getUniqueId(), now);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (plugin.configService().chatTrigger()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> markIfTracked(event.getPlayer(), Instant.now()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        ConfigService config = plugin.configService();
        if (!config.commandsTrigger() || !config.commandTrackingEnabled()) {
            markIfTracked(event.getPlayer(), Instant.now());
            return;
        }
        Instant now = Instant.now();
        if (!ensureTrackingState(event.getPlayer(), now)) {
            return;
        }
        CommandRootExtractor.extractAllowedRoot(event.getMessage(), config.excludedCommands())
                .ifPresent(ignored -> plugin.sessionService().incrementCommands(event.getPlayer().getUniqueId(), now));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (plugin.configService().inventoryTrigger() && event.getWhoClicked() instanceof Player player) {
            markIfTracked(player, Instant.now());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.configService().blockInteractionTrigger()) {
            markIfTracked(event.getPlayer(), Instant.now());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (plugin.configService().entityInteractionTrigger()) {
            markIfTracked(event.getPlayer(), Instant.now());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Instant now = Instant.now();
        if (plugin.configService().teleportTrigger() && ensureTrackingState(event.getPlayer(), now)) {
            plugin.sessionService().incrementTeleports(event.getPlayer().getUniqueId(), now);
            plugin.databaseService().saveTeleport(teleportRecord(event, now))
                    .exceptionally(throwable -> {
                        plugin.getLogger().warning("Failed to persist teleport event for " + event.getPlayer().getName() + ": " + throwable.getMessage());
                        return null;
                    });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (plugin.configService().gamemodeChangeTrigger() && ensureTrackingState(event.getPlayer(), Instant.now())) {
            plugin.sessionService().incrementGamemodeChanges(event.getPlayer().getUniqueId(), Instant.now());
        }
    }

    private void markIfTracked(Player player, Instant now) {
        if (ensureTrackingState(player, now)) {
            plugin.sessionService().markActivity(player.getUniqueId(), now);
        }
    }

    private boolean ensureTrackingState(Player player, Instant now) {
        boolean shouldTrack = player.hasPermission(plugin.configService().trackingPermission())
                || (plugin.configService().trackOperatorsAutomatically() && player.isOp());
        boolean currentlyTracked = plugin.sessionService().session(player.getUniqueId()).isPresent();
        if (shouldTrack) {
            plugin.sessionService().startSession(player.getUniqueId(), player.getName(), now);
            return true;
        }
        if (currentlyTracked) {
            plugin.sessionService().closeSession(player.getUniqueId(), now, SessionCloseReason.PERMISSION_REMOVED)
                    .ifPresent(plugin::persistClosedSession);
        }
        return false;
    }

    private boolean isMeaningfulMovement(Location from, Location to, ConfigService config) {
        if (to == null) {
            return false;
        }
        if (config.movementTrigger() && from.getWorld() == to.getWorld()
                && from.distanceSquared(to) >= config.movementThresholdSquared()) {
            return true;
        }
        return config.rotationTrigger()
                && (rotationDelta(from.getYaw(), to.getYaw()) >= config.rotationThresholdDegrees()
                || Math.abs(from.getPitch() - to.getPitch()) >= config.rotationThresholdDegrees());
    }

    private float rotationDelta(float left, float right) {
        float delta = Math.abs(left - right) % 360.0f;
        return delta > 180.0f ? 360.0f - delta : delta;
    }

    private TeleportRecord teleportRecord(PlayerTeleportEvent event, Instant now) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        return new TeleportRecord(
                player.getUniqueId(),
                player.getName(),
                now,
                event.getCause().name(),
                worldName(from),
                from.getX(),
                from.getY(),
                from.getZ(),
                worldName(to),
                to.getX(),
                to.getY(),
                to.getZ(),
                isVanished(player)
        );
    }

    private String worldName(Location location) {
        World world = location.getWorld();
        return world == null ? "unknown" : world.getName();
    }

    private boolean isVanished(Player player) {
        return player.getMetadata("vanished").stream().anyMatch(metadata -> metadata.asBoolean())
                || player.getMetadata("vanish").stream().anyMatch(metadata -> metadata.asBoolean());
    }
}
