package proteruga.DonationAlertsApi;

import me.clip.placeholderapi.libs.kyori.adventure.util.Ticks;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class DonationListener implements Listener {

    private final DonationAlertsApi plugin;

    private final static String spacePattern = Pattern.quote(" ");
    private final static String colonPattern = Pattern.quote(":");
    private final static Map<String, Consumer<String>> functions = Map.of(
            "sound", DonationListener::sound,
            "message", DonationListener::message,
            "title", DonationListener::title,
            "subtitle", DonationListener::subtitle,
            "times", DonationListener::times
    );

    public DonationListener(DonationAlertsApi plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDonation(DonationEvent e) {
        if (plugin.isDebug()) plugin.getLogger().info(DonationAlertsApi.CONSOLE_PREFIX + e.toString());
        if (!plugin.allowBuiltInCommands()) return;

        for (String command : plugin.getCommands()) {
            for (Map.Entry<String, Consumer<String>> entry : functions.entrySet()) {
                if (!command.startsWith("[" + entry.getKey() + "]")) continue;
                entry.getValue().accept(setPlaceholders(command, e, false));
                break;
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), setPlaceholders(command, e, true));
        }
    }

    private String setPlaceholders(String command, DonationEvent event, boolean quotesEscape) {
        String result;
        if (quotesEscape) {
            result = command
                    .replace("$sender$", event.getUsername().replace("\"", "\\\""))
                    .replace("$message$", event.getMessage().replace("\"", "\\\""))
                    .replace("$amount$", String.valueOf(event.getAmount()))
                    .replace("$currency$", event.getCurrency())
                    .replace("$recipient_amount$", String.valueOf(event.getAmountInUserCurrency()));
        }
        else {
            result = command
                    .replace("$sender$", event.getUsername())
                    .replace("$message$", event.getMessage())
                    .replace("$amount$", String.valueOf(event.getAmount()))
                    .replace("$currency$", event.getCurrency())
                    .replace("$recipient_amount$", String.valueOf(event.getAmountInUserCurrency()));
        }

        return SafePAPI.setPlaceholders(result);
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

    private static void message(String command) {
        String[] splitResult = command.split(spacePattern);

        if (splitResult.length < 3) {
            DonationAlertsApi.log(Level.WARNING, "Failed to parse function: " + command);
            return;
        }

        String target = splitResult[1];
        Audience audience = Audience.audience();
        if (target.equals("@a")) {
            audience = Audience.audience(Bukkit.getOnlinePlayers());
        }
        else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) audience = Audience.audience(player);
        }

        StringBuilder stringBuilder = new StringBuilder(splitResult[2]);
        for (int i = 3; i < splitResult.length; i++) {
            stringBuilder.append(" ").append(splitResult[i]);
        }
        String message = stringBuilder.toString();

        audience.sendMessage(MiniMessage.miniMessage().deserialize(message));
        DonationAlertsApi.log(Level.INFO, "The message \"" + message + "\" has been successfully sent to " + target);
    }

    private static void title(String command) {
        String[] splitResult = command.split(spacePattern);

        if (splitResult.length < 2) {
            DonationAlertsApi.log(Level.WARNING, "Failed to parse function: " + command);
            return;
        }

        String target = splitResult[1];
        Audience audience = Audience.audience();
        if (target.equals("@a")) {
            audience = Audience.audience(Bukkit.getOnlinePlayers());
        }
        else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) audience = Audience.audience(player);
        }

        String message;
        if (splitResult.length == 2) {
            message = "";
        }
        else {
            StringBuilder stringBuilder = new StringBuilder(splitResult[2]);
            for (int i = 3; i < splitResult.length; i++) {
                stringBuilder.append(" ").append(splitResult[i]);
            }
            message = stringBuilder.toString();
        }

        audience.sendTitlePart(TitlePart.TITLE, MiniMessage.miniMessage().deserialize(message));
        DonationAlertsApi.log(Level.INFO, "The title \"" + message + "\" has been successfully shown to " + target);
    }

    private static void subtitle(String command) {
        String[] splitResult = command.split(spacePattern);

        if (splitResult.length < 3) {
            DonationAlertsApi.log(Level.WARNING, "Failed to parse function: " + command);
            return;
        }

        String target = splitResult[1];
        Audience audience = Audience.audience();
        if (target.equals("@a")) {
            audience = Audience.audience(Bukkit.getOnlinePlayers());
        }
        else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) audience = Audience.audience(player);
        }

        StringBuilder stringBuilder = new StringBuilder(splitResult[2]);
        for (int i = 3; i < splitResult.length; i++) {
            stringBuilder.append(" ").append(splitResult[i]);
        }
        String message = stringBuilder.toString();

        audience.sendTitlePart(TitlePart.SUBTITLE, MiniMessage.miniMessage().deserialize(message));
        DonationAlertsApi.log(Level.INFO, "The subtitle \"" + message + "\" has been successfully shown to " + target);
    }

    private static void times(String command) {
        String[] splitResult = command.split(spacePattern);

        if (splitResult.length < 5) {
            DonationAlertsApi.log(Level.WARNING, "Failed to parse function: " + command);
            return;
        }

        String target = splitResult[1];
        Audience audience = Audience.audience();
        if (target.equals("@a")) {
            audience = Audience.audience(Bukkit.getOnlinePlayers());
        }
        else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) audience = Audience.audience(player);
        }

        long fadeIn = 10;
        try { fadeIn = Long.parseLong(splitResult[2]); }
        catch (NumberFormatException e) { DonationAlertsApi.log(Level.WARNING, "Failed to parse fadeIn time: " + e.getMessage()); }

        long stay = 100;
        try { stay = Long.parseLong(splitResult[3]); }
        catch (NumberFormatException e) { DonationAlertsApi.log(Level.WARNING, "Failed to parse stay time: " + e.getMessage()); }

        long fadeOut = 10;
        try { fadeOut = Long.parseLong(splitResult[4]); }
        catch (NumberFormatException e) { DonationAlertsApi.log(Level.WARNING, "Failed to parse fadeOut time: " + e.getMessage()); }


        audience.sendTitlePart(TitlePart.TIMES, Title.Times.times(
                Duration.of(fadeIn * Ticks.SINGLE_TICK_DURATION_MS, ChronoUnit.MILLIS),
                Duration.of(stay * Ticks.SINGLE_TICK_DURATION_MS, ChronoUnit.MILLIS),
                Duration.of(fadeOut * Ticks.SINGLE_TICK_DURATION_MS, ChronoUnit.MILLIS)
        ));
        DonationAlertsApi.log(Level.INFO, "The times of title has been successfully applied to " + target);
    }
}
