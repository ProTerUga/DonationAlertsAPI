package proteruga.DonationAlertsApi.BasicCommand;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proteruga.DonationAlertsApi.DonationAlertsApi;
import proteruga.DonationAlertsApi.DonationEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DonationCommand implements CommandExecutor, TabCompleter {
    private final DonationAlertsApi plugin;

    private static final List<String> SUB_COMMANDS = List.of("reload", "status", "auth", "token", "test");
    private static final List<String> CURRENCIES = List.of("BRL", "BYN", "EUR", "KZT", "PLN", "RUB", "TRY", "UAH", "USD", "UZS");

    public DonationCommand(DonationAlertsApi plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            plugin.sendMessage(sender, plugin.getMessage("command.usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> reloadCommand(sender);
            case "status" -> statusCommand(sender);
            case "auth" -> authCommand(sender);
            case "token" -> tokenCommand(sender, args);
            case "test" -> testCommand(sender, args);
            default -> plugin.sendMessage(sender, plugin.getMessage("command.unknown"));
        }

        return true;
    }

    private void reloadCommand(CommandSender sender) {
        if (!sender.hasPermission("daapi.reload")) {
            plugin.sendMessage(sender, plugin.getMessage("command.dont-have-permission"));
            return;
        }

        if (!plugin.readConfig()) {
            plugin.sendMessage(sender, plugin.getMessage("reload.error"));
            return;
        }
        plugin.sendMessage(sender, plugin.getMessage("reload.successfully-reloaded"));

        boolean trying = plugin.tryConnect();

        if (!trying) {
            plugin.sendMessage(sender, plugin.getMessage("reload.missing-token"));
            return;
        }

        plugin.sendMessage(sender, plugin.getMessage("reload.reconnect-trying"));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.sendMessage(sender, plugin.getMessage(
                    plugin.isConnected()
                            ? "reload.successfully-connected"
                            : "reload.connection-failed")
            );
        }, 20L * 5);
    }

    private void statusCommand(CommandSender sender) {
        if (!sender.hasPermission("daapi.status")) {
            plugin.sendMessage(sender, plugin.getMessage("command.dont-have-permission"));
            return;
        }

        plugin.sendMessage(sender, plugin.getMessage(plugin.isConnected()
                ? "status.connected"
                : "status.not-connected")
        );
    }

    private void authCommand(CommandSender sender) {
        if (!sender.hasPermission("daapi.auth")) {
            plugin.sendMessage(sender, plugin.getMessage("command.dont-have-permission"));
            return;
        }

        String url = plugin.generateAuthUrl();

        if (url == null) {
            plugin.sendMessage(sender, plugin.getMessage("auth.missing-client-id"));
            return;
        }

        plugin.sendMessage(sender, 
                MiniMessage.miniMessage().deserialize(
                        MiniMessage.miniMessage().serialize(
                                plugin.getMessage("auth.url-message")
                        ).replaceAll(Pattern.quote("$url$"), url)
                )
        );
    }

    private void tokenCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("daapi.token")) {
            plugin.sendMessage(sender, plugin.getMessage("command.dont-have-permission"));
            return;
        }

        if (args.length < 2) {
            plugin.sendMessage(sender, plugin.getMessage("token.usage"));
            return;
        }

        String code = args[1];

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.exchangeCodeForToken(code);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.sendMessage(sender, plugin.getMessage(success
                        ? "token.successfully-obtained"
                        : "token.failed"
                ));
            });
        });
    }
    private void testCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("daapi.test")) {
            plugin.sendMessage(sender, plugin.getMessage("command.dont-have-permission"));
            return;
        }

        if (args.length < 5) {
            plugin.sendMessage(sender, plugin.getMessage("test.usage"));
            return;
        }

        String username = args[1];
        float amount;
        try {
            amount = Float.parseFloat(args[2]);
        } catch (NumberFormatException e) {
            plugin.sendMessage(sender, plugin.getMessage("test.number-format-error"));
            return;
        }
        String currency = args[3];
        String message;
        if (!args[4].startsWith("\"")) message = args[4];
        else {
            if (args.length == 5) {
                message = args[4].substring(1, args[4].length() - 1);
            } else {
                StringBuilder stringBuilder = new StringBuilder(args[4].substring(1));
                int i;
                for (i = 5; i < args.length - 1; i++) {
                    stringBuilder.append(" ").append(args[i]);
                }
                if (args[i].endsWith("\"")) {
                    stringBuilder.append(" ");
                    stringBuilder.append(args[i], 0, args[i].length() - 1);
                }
                message = stringBuilder.toString();
            }
        }

        if (!plugin.isConnected()) {
            plugin.sendMessage(sender, plugin.getMessage("test.connection-warning"));
        }

        plugin.sendMessage(sender, plugin.getMessage("test.donation"));
        Bukkit.getServer().getPluginManager().callEvent(
                new DonationEvent(
                        -1, "Donations", username, message, "text", "null",
                        amount, currency, 1, -1, "null", null,
                        LocalDateTime.now().format(DonationEvent.dateTimeFormatter), "null", "default"
                )
        );
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filterStartingWith(SUB_COMMANDS, args[0]);
        }

        if (args.length >= 2) {
            switch (args[0].toLowerCase()) {
                case "token": {
                    if (args.length == 2) return List.of("<Enter your code here>");
                    break;
                }
                case "test": {
                    if (args.length == 2) return null;
                    if (args.length == 3 && args[2].isBlank()) return List.of("<Amount>");
                    if (args.length == 4) return filterStartingWith(CURRENCIES, args[3]);
                    if (args.length == 5 && args[4].isBlank()) return List.of("\"Donation message...\"");
                    break;
                }
            }
        }

        return List.of();
    }

    private List<String> filterStartingWith(List<String> source, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : source) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}
