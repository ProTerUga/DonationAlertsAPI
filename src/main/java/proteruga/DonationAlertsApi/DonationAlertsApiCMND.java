package proteruga.DonationAlertsApi;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings(value = "all")
public class DonationAlertsApiCMND {
    private static DonationAlertsApi plugin;
    public static LiteralCommandNode<CommandSourceStack> command(DonationAlertsApi plugin) {
        DonationAlertsApiCMND.plugin = plugin;
        return Commands.literal("daapi")
                .then(Commands.literal("reload")
                        .requires(ctx -> ctx.getSender().hasPermission("daapi.reload"))
                        .executes(DonationAlertsApiCMND::reload))
                .then(Commands.literal("status")
                        .requires(ctx -> ctx.getSender().hasPermission("daapi.status"))
                        .executes(DonationAlertsApiCMND::status))
                .then(Commands.literal("auth")
                        .requires(ctx -> ctx.getSender().hasPermission("daapi.auth"))
                        .executes(DonationAlertsApiCMND::auth))
                .then(Commands.literal("token")
                        .requires(ctx -> ctx.getSender().hasPermission("daapi.token"))
                        .then(Commands.argument("code", StringArgumentType.string())
                                .requires(ctx -> ctx.getSender().hasPermission("daapi.token"))
                                .executes(DonationAlertsApiCMND::token)))
                .then(Commands.literal("test")
                        .requires(ctx -> ctx.getSender().hasPermission("daapi.test"))
                        .then(Commands.argument("username", StringArgumentType.string())
                                .requires(ctx -> ctx.getSender().hasPermission("daapi.test"))
                                .suggests((ctx, b) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> b.suggest(p.getName()));
                                    return b.buildFuture();
                                })
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .requires(ctx -> ctx.getSender().hasPermission("daapi.test"))
                                        .suggests((ctx, b) -> {
                                            List.of(1, 2, 5, 10, 20, 50, 100).forEach(b::suggest);
                                            return b.buildFuture();
                                        })
                                        .then(Commands.argument("currency", StringArgumentType.word())
                                                .requires(ctx -> ctx.getSender().hasPermission("daapi.test"))
                                                .suggests((ctx, b) -> {
                                                    List.of("RUB", "USD", "EUR", "KZT").forEach(b::suggest);
                                                    return b.buildFuture();
                                                })
                                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                                        .suggests((ctx, b) -> {
                                                            b.suggest("Message...");
                                                            return b.buildFuture();
                                                        })
                                                        .requires(ctx -> ctx.getSender().hasPermission("daapi.test"))
                                                        .executes(DonationAlertsApiCMND::test))))))
                .build();
    }
    private static int reload(CommandContext<CommandSourceStack> ctx) {
        plugin.reloadConfig();
        plugin.readConfig();
        ctx.getSource().getSender().sendMessage(plugin.getMessage("reload.successfully-reloaded"));

        boolean trying = plugin.tryConnect();

        if (!trying) {
            ctx.getSource().getSender().sendMessage(plugin.getMessage("reload.missing-token"));
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().getSender().sendMessage(plugin.getMessage("reload.reconnect-trying"));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.isConnected()) {
                ctx.getSource().getSender().sendMessage(plugin.getMessage("reload.successfully-connected"));
            } else {
                ctx.getSource().getSender().sendMessage(plugin.getMessage("reload.connection-failed"));
            }
        }, 20L * 5);

        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        if (plugin.isConnected()) {
            ctx.getSource().getSender().sendMessage(plugin.getMessage("status.connected"));
        } else {
            ctx.getSource().getSender().sendMessage(plugin.getMessage("status.not-connected"));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int test(CommandContext<CommandSourceStack> ctx) {
        if (!plugin.isConnected()) {
            ctx.getSource().getSender().sendMessage(plugin.getMessage("test.connection-warning"));
        }

        ctx.getSource().getSender().sendMessage(plugin.getMessage("test.donation"));
        Bukkit.getServer().getPluginManager().callEvent(
            new DonationEvent(
                    -1, "Donations", ctx.getArgument("username", String.class),
                    ctx.getArgument("message", String.class), "text", "null",
                    ctx.getArgument("amount", Float.class), ctx.getArgument("currency", String.class),
                    1, -1, "null", null,
                    LocalDateTime.now().format(DonationEvent.dateTimeFormatter), "null", "default"
            )
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int auth(CommandContext<CommandSourceStack> ctx) {
        String url = plugin.generateAuthUrl();

        if (url == null) {
            ctx.getSource().getSender().sendMessage(plugin.getMessage("auth.missing-client-id"));
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().getSender().sendMessage(
                MiniMessage.miniMessage().deserialize(
                        MiniMessage.miniMessage().serialize(
                                plugin.getMessage("auth.url-message")
                        ).replaceAll(Pattern.quote("$url$"), url)
                )
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int token(CommandContext<CommandSourceStack> ctx) {
        String code = ctx.getArgument("code", String.class);
        if (code.isBlank()) {
            ctx.getSource().getSender().sendMessage(plugin.getMessage("token.empty-code"));
            return Command.SINGLE_SUCCESS;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.exchangeCodeForToken(code);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    ctx.getSource().getSender().sendMessage(plugin.getMessage("token.successfully-obtained"));
                } else {
                    ctx.getSource().getSender().sendMessage(plugin.getMessage("token.failed"));
                }
            });
        });
        return Command.SINGLE_SUCCESS;
    }
}
