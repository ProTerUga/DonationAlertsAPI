package proteruga.DonationAlertsApi;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.regex.Pattern;

public class DonationListener implements Listener {

    public static final String SENDER_PLACEHOLDER = Pattern.quote("$sender$");
    public static final String MESSAGE_PLACEHOLDER = Pattern.quote("$message$");
    public static final String CURRENCY_PLACEHOLDER = Pattern.quote("$currency$");
    public static final String AMOUNT_PLACEHOLDER = Pattern.quote("$amount$");

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
                        .replaceAll(SENDER_PLACEHOLDER, e.getUsername())
                        .replaceAll(MESSAGE_PLACEHOLDER, e.getMessage())
                        .replaceAll(AMOUNT_PLACEHOLDER, String.valueOf(e.getAmount()))
                        .replaceAll(CURRENCY_PLACEHOLDER, e.getCurrency());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }
}
