package proteruga.DonationAlertsApi;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class DonationListener implements Listener {

    private final DonationAlertsApi plugin;
    private final List<String> commands;

    private final static String spacePattern = Pattern.quote(" ");
    private final static String colonPattern = Pattern.quote(":");
    private final static Map<String, Consumer<String>> functions = Map.of(
            "sound", DonationListener::sound
    );

    public DonationListener(DonationAlertsApi plugin, List<String> commands) {
        this.plugin = plugin;
        this.commands = commands;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDonation(DonationEvent e) {
        if (plugin.isDebug()) plugin.getLogger().info(DonationAlertsApi.CONSOLE_PREFIX + e.toString());
        if (!plugin.allowBuiltInCommands()) return;

        for (String command : commands) {
            command = command
                    .replace("$sender$", e.getUsername().replace("\"", "\\\""))
                    .replace("$message$", e.getMessage().replace("\"", "\\\""))
                    .replace("$amount$", String.valueOf(e.getAmount()))
                    .replace("$currency$", e.getCurrency())
                    .replace("$recipient_amount$", String.valueOf(e.getAmountInUserCurrency()));
            command = SafePAPI.setPlaceholders(command);

            for (Map.Entry<String, Consumer<String>> entry : functions.entrySet()) {
                if (!command.startsWith("[" + entry.getKey() + "]")) continue;
                entry.getValue().accept(command);
                return;
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    private static void sound(String command) {
        String[] splitResult = command.split(spacePattern);

        if (splitResult.length < 2) {
            DonationAlertsApi.log(Level.WARNING, "Failed to parse function: " + command);
            return;
        }

        String[] args = { null, null, "MASTER", "1", "1", "@a" };
        System.arraycopy(splitResult, 0, args, 0, Math.min(splitResult.length, args.length));

        String[] soundId = args[1].split(colonPattern);
        String soundNamespace = soundId[0];
        String soundValue = soundId[1];

        Sound.Source source = Sound.Source.MASTER;
        try { source = Sound.Source.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException e) { DonationAlertsApi.log(Level.WARNING, "Failed to parse sound source: " + e.getMessage()); }

        float volume = 1;
        try { volume = Float.parseFloat(args[3]); }
        catch (NumberFormatException e) { DonationAlertsApi.log(Level.WARNING, "Failed to parse sound volume: " + e.getMessage()); }

        float pitch = 1;
        try { pitch = Float.parseFloat(args[4]); }
        catch (NumberFormatException e) { DonationAlertsApi.log(Level.WARNING, "Failed to parse sound pitch: " + e.getMessage()); }

        String target = args[5];
        Audience audience = Audience.audience();
        if (target.equals("@a")) {
            audience = Audience.audience(Bukkit.getOnlinePlayers());
        }
        else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) audience = Audience.audience(player);
        }

        Sound sound = Sound.sound(Key.key(soundNamespace, soundValue), source, volume, pitch);
        audience.playSound(sound);
        DonationAlertsApi.log(Level.INFO, "The sound " + args[1] + " in the category " + args[2] + " was successfully played at volume " + args[3] + " and pitch " + args[4] + " for " + args[5]);
    }
}
