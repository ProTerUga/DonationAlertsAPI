package proteruga.DonationAlertsApi;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class DonationListener implements Listener {

    private final DonationAlertsApi plugin;
    private final List<String> commands;

    public DonationListener(DonationAlertsApi plugin, List<String> commands) {
        this.plugin = plugin;
        this.commands = commands;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDonation(DonationEvent e) {
        if (plugin.isDebug()) plugin.getLogger().info(DonationAlertsApi.CONSOLE_PREFIX + e.toString());
        if (plugin.allowBuiltInCommands()) {
            for (String command : commands) {
                command = command
                        .replace("$sender$", e.getUsername().replace("\"", "\\\""))
                        .replace("$message$", e.getMessage().replace("\"", "\\\""))
                        .replace("$amount$", String.valueOf(e.getAmount()))
                        .replace("$currency$", e.getCurrency())
                        .replace("$recipient_amount$", String.valueOf(e.getAmountInUserCurrency()));
                command = SafePAPI.setPlaceholders(command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }
}
