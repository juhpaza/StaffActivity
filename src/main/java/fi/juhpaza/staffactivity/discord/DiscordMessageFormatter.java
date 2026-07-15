package fi.juhpaza.staffactivity.discord;

import fi.juhpaza.staffactivity.model.StaffReportEntry;
import fi.juhpaza.staffactivity.util.DurationFormatter;
import java.util.List;

/**
 * Builds Discord-safe plain text messages for StaffActivity webhooks.
 */
public final class DiscordMessageFormatter {
    public String testMessage(String serverName) {
        return "**StaffActivity testi**\nWebhook toimii palvelimella `" + serverName + "`.";
    }

    public String staffJoin(String playerName) {
        return "**Staff liittyi:** `" + playerName + "`";
    }

    public String staffQuit(String playerName) {
        return "**Staff poistui:** `" + playerName + "`";
    }

    public String pluginError(String message) {
        return "**StaffActivity virhe**\n" + message;
    }

    public String report(String title, String range, List<StaffReportEntry> entries) {
        StringBuilder builder = new StringBuilder();
        builder.append("**").append(title).append("**\n");
        builder.append(range).append("\n");
        if (entries.isEmpty()) {
            builder.append("Ei tallennettua staff-aktiivisuutta.");
            return builder.toString();
        }
        int index = 1;
        for (StaffReportEntry entry : entries) {
            builder.append("\n")
                    .append(index++)
                    .append(". `")
                    .append(entry.latestName())
                    .append("` ")
                    .append(DurationFormatter.seconds(entry.onlineSeconds()))
                    .append(" online, ")
                    .append(DurationFormatter.seconds(entry.activeSeconds()))
                    .append(" active, ")
                    .append(DurationFormatter.seconds(entry.afkSeconds()))
                    .append(" AFK, ")
                    .append(entry.activityPercent())
                    .append("% aktiivinen")
                    .append("\n")
                    .append("   Komennot: ")
                    .append(entry.commandCount())
                    .append(" | Teleportit: ")
                    .append(entry.teleportCount())
                    .append(" | Toimet: ")
                    .append(entry.staffActionCount());
        }
        return builder.toString();
    }
}
