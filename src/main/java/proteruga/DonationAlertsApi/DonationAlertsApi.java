package proteruga.DonationAlertsApi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import proteruga.DonationAlertsApi.BasicCommand.DonationCommand;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class DonationAlertsApi extends JavaPlugin {

    public final static String CONSOLE_PREFIX = "[DonationAlertsAPI] ";
    private final static String PATTERN_NEWLINE = Pattern.quote("\n");

    private static DonationAlertsApi plugin;
    private static boolean placeholdersEnabled = false;

    private final List<String> commands = new ArrayList<>();
    private final Map<String, Component> messages = new HashMap<>();

    private String accessToken;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private int reconnectDelay;
    private boolean logInfo;
    private boolean logDonations;
    private boolean logWebSocket;
    private boolean builtInCommands;
    private boolean debug;


    private String socketToken;
    private long userId;
    private WebSocketClient wsClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Gson gson = new Gson();
    private final AtomicInteger msgId = new AtomicInteger(1);

    @Override
    public void onEnable() {
        plugin = this;

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholdersEnabled = true;
            getLogger().info(CONSOLE_PREFIX + "PlaceholderAPI plugin detected, placeholder support available.");
        } else {
            getLogger().info(CONSOLE_PREFIX + "PlaceholderAPI plugin not found, launching without placeholder support.");
        }

        saveDefaultConfig();
        if (!readConfig()) {
            getLogger().severe(CONSOLE_PREFIX + "Failed to read configuration. Check the console for errors.");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        Bukkit.getPluginManager().registerEvents(new DonationListener(this), this);

        DonationCommand donationCommand = new DonationCommand(this);
        PluginCommand pluginCommand = getCommand("donationalertsapi");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(donationCommand);
            pluginCommand.setTabCompleter(donationCommand);
        }
        else getLogger().severe(CONSOLE_PREFIX + "Could not register a command.");

        tryConnect();
    }

    @Override
    public void onDisable() {
        if (wsClient != null) wsClient.close();
        scheduler.shutdownNow();
    }

    public boolean readConfig() {
        InputStream configResource = this.getResource("config.yml");

        if (configResource == null) {
            this.getLogger().severe(CONSOLE_PREFIX + "Failed to get config.yml from resources of this plugin.");
            return false;
        }

        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(configResource, StandardCharsets.UTF_8));

        File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            this.saveResource("config.yml", false);
        }

        YamlConfiguration currentConfig = new YamlConfiguration();
        try {
            currentConfig.load(configFile);
        } catch (InvalidConfigurationException e) {
            this.getLogger().severe(CONSOLE_PREFIX + "Yaml parsing error: " + e.getMessage());
            return false;
        } catch (FileNotFoundException e) {
            this.getLogger().severe(CONSOLE_PREFIX + "File not found: " + e.getMessage());
            return false;
        } catch (IOException e) {
            this.getLogger().severe(CONSOLE_PREFIX + "File error: " + e.getMessage());
            return false;
        }

        boolean needSave = false;
        for (String key : defaultConfig.getKeys(true)) {
            if (currentConfig.contains(key)) continue;
            currentConfig.set(key, defaultConfig.get(key));
            needSave = true;
        }

        for (String key : currentConfig.getKeys(true)) {
            if (defaultConfig.contains(key)) continue;
            List<String> comments = List.of(
                    "[InvalidSection]",
                    "The \"" + key + "\" section is not defined in the current version of the plugin.",
                        "Check the documentation or spelling of this section."
            );

            if (currentConfig.getComments(key).contains("[InvalidSection]")) continue;
            currentConfig.setComments(key, comments);
            needSave = true;
        }

        if (needSave) {
            try {
                currentConfig.save(configFile);
            } catch (IOException e) {
                this.getLogger().severe(CONSOLE_PREFIX + "Could not save config: " + e.getMessage());
            }
        }
        debug = currentConfig.getBoolean("debug", false);

        accessToken = currentConfig.getString("access-token", "");
        clientId = currentConfig.getString("client-id", "");
        clientSecret = currentConfig.getString("client-secret", "");
        redirectUri = currentConfig.getString("redirect-uri", "http://localhost:8080/callback");

        reconnectDelay = currentConfig.getInt("reconnect-delay", 10);
        logDonations = currentConfig.getBoolean("log-donations", true);
        logInfo = currentConfig.getBoolean("log-info", true);
        logWebSocket = currentConfig.getBoolean("log-web-socket", false);
        builtInCommands = currentConfig.getBoolean("enable-builtin-commands", false);

        if (builtInCommands) {
            commands.clear();
            commands.addAll(currentConfig.getStringList("commands"));
            if (debug) log(Level.INFO, commands.size() + " commands were loaded.");
        }


        ConfigurationSection messagesSection = currentConfig.getConfigurationSection("messages");

        if (messagesSection != null) {
            messages.clear();
            String prefix = currentConfig.getString("messages.prefix", "Missing messages.prefix ");
            for (String key : messagesSection.getKeys(true)) {
                if (key.equals("prefix")) continue;
                messages.put(key, MiniMessage.miniMessage().deserialize(prefix + messagesSection.getString(key)));
            }
        }
        else getLogger().severe(CONSOLE_PREFIX + "Missing messages section in config.yml!");
        return true;
    }

    public boolean tryConnect() {
        if (accessToken.isBlank()) {
            getLogger().severe(CONSOLE_PREFIX + "For DonationAlertsAPI to work, you must specify the access-token in config.yml.");
            return false;
        }
        else {
            if (wsClient != null) {
                wsClient.close();
                wsClient = null;
            }
            getServer().getScheduler().runTaskAsynchronously(this, this::fetchUserDataAndConnect);
            return true;
        }
    }

    private void fetchUserDataAndConnect() {
        getLogger().info(CONSOLE_PREFIX + "Getting user data...");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.donationalerts.com/api/v1/user/oauth"))
                    .header("Authorization", "Bearer " + accessToken)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                getLogger().severe(CONSOLE_PREFIX + "User receipt error: " + response.body());
                return;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject data = json.getAsJsonObject("data");
            userId = data.get("id").getAsLong();
            socketToken = data.get("socket_connection_token").getAsString();

            if (logInfo) getLogger().info(CONSOLE_PREFIX + "User " + userId + ", socket_token received");
            connectCentrifugo();

        } catch (Exception e) {
            getLogger().severe(CONSOLE_PREFIX + "Error receiving user data: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private void connectCentrifugo() {
        try {
            URI uri = new URI("wss://centrifugo.donationalerts.com/connection/websocket");
            wsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    if (logWebSocket) getLogger().info(CONSOLE_PREFIX + "WebSocket connection to the Centrifugo is established");
                    sendAuthMessage();
                }

                @Override
                public void onMessage(String message) {
                    if (logWebSocket) getLogger().info(CONSOLE_PREFIX + "WebSocket: " + message.replaceAll(PATTERN_NEWLINE, ""));
                    handleCentrifugoMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (logWebSocket) getLogger().warning(CONSOLE_PREFIX + "WebSocket closed: " + reason.replaceAll(PATTERN_NEWLINE, ""));
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception e) {
                    if (logWebSocket) getLogger().severe(CONSOLE_PREFIX + "WebSocket error: " + e.getMessage());
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            getLogger().severe(CONSOLE_PREFIX + "Couldn't connect to the Centrifugo: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private void sendAuthMessage() {
        JsonObject msg = new JsonObject();
        JsonObject params = new JsonObject();
        params.addProperty("token", socketToken);
        msg.add("params", params);
        msg.addProperty("id", msgId.getAndIncrement());

        wsClient.send(gson.toJson(msg));
        if (logInfo) getLogger().info(CONSOLE_PREFIX + "Authentication request sent to the Centrifugo");
    }

    private void handleCentrifugoMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();

            if (json.has("result") && json.getAsJsonObject("result").has("client")) {
                String clientId = json.getAsJsonObject("result").get("client").getAsString();
                if (logInfo) getLogger().info(CONSOLE_PREFIX + "Client ID received: " + clientId);
                subscribeToDonationChannel(clientId);
                return;
            }

            if (json.has("result") && json.getAsJsonObject("result").has("type") &&
                    json.getAsJsonObject("result").get("type").getAsInt() == 1) {
                if (logInfo) getLogger().info(CONSOLE_PREFIX + "Channel subscription confirmed");
                return;
            }

            JsonObject donationData = null;

            if (json.has("result") && json.getAsJsonObject("result").has("data")) {
                JsonObject resultData = json.getAsJsonObject("result").getAsJsonObject("data");
                if (resultData.has("data") && resultData.get("data").isJsonObject()) {
                    donationData = resultData.getAsJsonObject("data");
                } else {
                    donationData = resultData;
                }
            }
            else if (json.has("data")) {
                donationData = json.getAsJsonObject("data");
            }
            else if (json.has("push") && json.getAsJsonObject("push").has("data")) {
                donationData = json.getAsJsonObject("push").getAsJsonObject("data");
            }

            if (donationData != null && donationData.has("donation")) {
                donationData = donationData.getAsJsonObject("donation");
            }

            if (donationData != null) {
                parseDonation(donationData);
            } else {
                if (logWebSocket) getLogger().warning(CONSOLE_PREFIX + "Couldn't extract donation data from the message");
            }

        } catch (Exception e) {
            getLogger().severe(CONSOLE_PREFIX + "Error processing the Centrifugo message: " + e.getMessage());
        }
    }

    private void subscribeToDonationChannel(String clientId) {
        try {
            String channel = "$alerts:donation_" + userId;
            JsonObject body = new JsonObject();
            body.addProperty("client", clientId);
            JsonArray channels = new JsonArray();
            channels.add(channel);
            body.add("channels", channels);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.donationalerts.com/api/v1/centrifuge/subscribe"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                getLogger().severe(CONSOLE_PREFIX + "Channel subscription error: " + response.body());
                return;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray channelData = json.getAsJsonArray("channels");
            if (!channelData.isEmpty()) {
                JsonObject first = channelData.get(0).getAsJsonObject();
                String channelToken = first.get("token").getAsString();
                sendSubscribeToChannel(channel, channelToken);
            }
        } catch (Exception e) {
            getLogger().severe(CONSOLE_PREFIX + "Subscription error: " + e.getMessage());
        }
    }

    private void sendSubscribeToChannel(String channel, String token) {
        JsonObject msg = new JsonObject();
        JsonObject params = new JsonObject();
        params.addProperty("channel", channel);
        params.addProperty("token", token);
        msg.add("params", params);
        msg.addProperty("method", 1);
        msg.addProperty("id", msgId.getAndIncrement());

        wsClient.send(gson.toJson(msg));
        if (logInfo) getLogger().info(CONSOLE_PREFIX + "A request has been sent to connect to the channel " + channel);
    }

    private void parseDonation(JsonObject donation) {
        try {
            long id = donation.get("id").getAsLong();
            String name = donation.get("name").getAsString();
            String username = donation.get("username").getAsString();
            String message = donation.has("message") ? donation.get("message").getAsString() : "";
            String messageType = donation.has("message_type") ? donation.get("message_type").getAsString() : "text";
            String payinSystem = donation.has("payin_system") && !donation.get("payin_system").isJsonNull()
                    ? donation.get("payin_system").getAsString() : null;
            double amount = donation.get("amount").getAsDouble();
            String currency = donation.get("currency").getAsString();
            int isShown = donation.get("is_shown").getAsInt();
            double amountInUserCurrency = donation.has("amount_in_user_currency")
                    ? donation.get("amount_in_user_currency").getAsDouble() : amount;
            String recipientName = donation.has("recipient_name") ? donation.get("recipient_name").getAsString() : "";
            String createdAt = donation.get("created_at").getAsString();
            String shownAt = donation.has("shown_at") && !donation.get("shown_at").isJsonNull()
                    ? donation.get("shown_at").getAsString() : null;
            String reason = donation.has("reason") ? donation.get("reason").getAsString() : "default";

            DonationEvent.RecipientInfo recipient = null;
            if (donation.has("recipient") && donation.get("recipient").isJsonObject()) {
                JsonObject recip = donation.getAsJsonObject("recipient");
                long userId = recip.get("user_id").getAsLong();
                String code = recip.get("code").getAsString();
                String nameRecip = recip.get("name").getAsString();
                String avatar = recip.has("avatar") ? recip.get("avatar").getAsString() : null;
                recipient = new DonationEvent.RecipientInfo(userId, code, nameRecip, avatar);
            }

            final DonationEvent.RecipientInfo finalRecipient = recipient;
            final String finalPayinSystem = payinSystem;
            final String finalShownAt = shownAt;
            final String finalReason = reason;
            final String finalMessageType = messageType;
            final double finalAmountInUserCurrency = amountInUserCurrency;
            final String finalRecipientName = recipientName;

            getServer().getScheduler().runTask(this, () -> {
                DonationEvent event = new DonationEvent(
                        id, name, username, message, finalMessageType,
                        finalPayinSystem, amount, currency, isShown,
                        finalAmountInUserCurrency, finalRecipientName, finalRecipient,
                        createdAt, finalShownAt, finalReason
                );
                getServer().getPluginManager().callEvent(event);
                if (logDonations) getLogger().info(CONSOLE_PREFIX + "Received a donation from " + username + " in the amount of " + amount + " " + currency + " (ID: " + id + ")");
            });

        } catch (Exception e) {
            getLogger().severe(CONSOLE_PREFIX + "Donation parsing error: " + e.getMessage());
        }
    }

    private void scheduleReconnect() {
        scheduler.schedule(() -> {
            if (wsClient == null || !wsClient.isOpen()) {
                getLogger().warning(CONSOLE_PREFIX + "WebSocket is not responding. Reconnecting...");
                fetchUserDataAndConnect();
            }
        }, reconnectDelay, TimeUnit.SECONDS);
    }


    public String generateAuthUrl() {
        if (clientId.isBlank()) {
            return null;
        }
        return "https://www.donationalerts.com/oauth/authorize?response_type=code&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=oauth-user-show%20oauth-donation-subscribe";
    }

    private String encodeParams(Map<String, String> params) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!result.isEmpty()) result.append("&");
            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return result.toString();
    }

    public boolean exchangeCodeForToken(String code) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            getLogger().severe(CONSOLE_PREFIX + "Client ID or Client Secret is not set in config.yml.");
            return false;
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", "authorization_code");
            params.put("client_id", clientId);
            params.put("client_secret", clientSecret);
            params.put("code", code);
            params.put("redirect_uri", redirectUri);

            String formData = encodeParams(params);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.donationalerts.com/oauth/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                getLogger().severe(CONSOLE_PREFIX + "Token exchange error (response " + response.statusCode() + "): " + response.body());
                return false;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String newToken = json.get("access_token").getAsString();
            getConfig().set("access-token", newToken);
            saveConfig();
            reloadConfig();
            accessToken = newToken;

            getLogger().info(CONSOLE_PREFIX + "Access token successfully updated! Reconnecting...");
            tryConnect();
            return true;

        } catch (Exception e) {
            getLogger().severe(CONSOLE_PREFIX + "Failed to exchange code: " + e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        return wsClient != null && wsClient.isOpen();
    }

    public @NotNull Component getMessage(@NotNull String key) {
        return messages.getOrDefault(key, Component.text("Missing message " + key).color(TextColor.fromCSSHexString("#ff5555")));
    }

    public void sendMessage(@NotNull CommandSender sender, Component message) {
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage(PlainTextComponentSerializer.plainText().serialize(message));
        } else {
            sender.sendMessage(message);
        }
    }

    public boolean allowBuiltInCommands() {
        return builtInCommands;
    }

    public List<String> getCommands() {
        return commands;
    }

    public boolean isDebug() {
        return debug;
    }

    public static boolean isPlaceholdersEnabled() {
        return placeholdersEnabled;
    }

    public static void log(Level level, String logMessage) {
        plugin.getLogger().log(level, CONSOLE_PREFIX + logMessage);
    }
}