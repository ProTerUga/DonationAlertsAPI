package proteruga.DonationAlertsApi;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

public class DonationEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final long id;
    private final String name;
    private final String username;
    private final String message;
    private final String messageType;
    private final String payinSystem;
    private final double amount;
    private final String currency;
    private final int isShown;
    private final double amountInUserCurrency;
    private final String recipientName;
    private final RecipientInfo recipient;
    private final String createdAt;
    private final String shownAt;
    private final String reason;

    public DonationEvent(long id, String name, String username, String message, String messageType,
                         String payinSystem, double amount, String currency, int isShown,
                         double amountInUserCurrency, String recipientName, RecipientInfo recipient,
                         String createdAt, String shownAt, String reason) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.message = message;
        this.messageType = messageType;
        this.payinSystem = payinSystem;
        this.amount = amount;
        this.currency = currency;
        this.isShown = isShown;
        this.amountInUserCurrency = amountInUserCurrency;
        this.recipientName = recipientName;
        this.recipient = recipient;
        this.createdAt = createdAt;
        this.shownAt = shownAt;
        this.reason = reason;
    }


    public long getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getMessage() { return message; }
    public String getMessageType() { return messageType; }
    public String getPayinSystem() { return payinSystem; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public int getIsShown() { return isShown; }
    public double getAmountInUserCurrency() { return amountInUserCurrency; }
    public String getRecipientName() { return recipientName; }
    public RecipientInfo getRecipient() { return recipient; }
    public String getCreatedAt() { return createdAt; }
    public String getShownAt() { return shownAt; }
    public String getReason() { return reason; }

    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public record RecipientInfo(long userId, String code, String name, String avatar) {
    }

    public static DonationEvent test(String username, String message, double amount, String currency) {
        return new DonationEvent(
                -1, null, username, message, null, null, amount, currency,
                -1, -1, null, null, null, null, null);
    }
}