package proteruga.DonationAlertsApi;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

/**
 * An event that is triggered when a new donation is received from the user.
 * <p>
 * It contains detailed information about the transaction: details of the payer, recipient,
 * amount, currency, text message and timestamps.
 * </p>
 * @author ProTerUga
 * @since 1.0.0*/
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

    /**
     * The donation event constructor.
     *
     * @param id The unique identifier of the donation.<br>
     * @param name The name of the notification (For donations: {@code Donations}).<br>
     * @param username The sender's nickname.<br>
     * @param message The text of the message attached to the donation.<br>
     * @param messageType The type of message (e.g. {@code text}, {@code voice}).<br>
     * @param payinSystem The code of the payment system (e.g. {@code PAYPAL}, {@code STRIPE}).<br>
     * @param amount The amount of the donation in the currency {@code currency}. Always more than zero.<br>
     * @param currency The payment currency code in the ISO 4217 format (e.g. {@code USD}, {@code RUB}, {@code EUR}).<br>
     * @param isShown The status of the donation display on the screen ({@code 1} - shown, {@code 0} - hidden).<br>
     * @param amountInUserCurrency The amount of the donation converted into the recipient's currency. Always more than zero.<br>
     * @param recipientName The displayed name of the recipient of the donation.<br>
     * @param recipient An object with full information about the recipient {@link RecipientInfo}.<br>
     * @param createdAt The date and time of donation creation in string format.<br>
     * @param shownAt The date and time when the donation was shown on the screen.<br>
     * @param reason The reason for the rejection or the donation status (e.g. {@code default}).<br>
     */
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

    /** Returns the unique identifier of the donation
     * @return the unique identifier of the donation*/
    public long getId() { return id; }

    /** Returns the name of the notification
     * @return name of the notification (For donations: {@code Donations})*/
    public String getName() { return name; }

    /** Returns the sender's nickname
     * @return the sender's nickname*/
    public String getUsername() { return username; }

    /** Returns the text of the message attached to the donation
     * @return the text of the message attached to the donation*/
    public String getMessage() { return message; }

    /** Returns the type of message
     * @return the type of message (e.g. {@code text}, {@code voice})*/
    public String getMessageType() { return messageType; }

    /** Returns the code of the payment system
     * @return the code of the payment system (e.g. {@code PAYPAL}, {@code STRIPE})*/
    public String getPayinSystem() { return payinSystem; }

    /** Returns the amount of the donation in the {@code currency}
     * @return the amount of the donation in the currency {@code currency}. Always more than zero*/
    public double getAmount() { return amount; }

    /** Returns the payment currency code
     * @return the payment currency code in the ISO 4217 format (e.g. {@code USD}, {@code RUB}, {@code EUR})*/
    public String getCurrency() { return currency; }

    /** Returns the status of the donation display on the screen
     * @return the status of the donation display on the screen ({@code 1} - shown, {@code 0} - hidden)*/
    public int getIsShown() { return isShown; }

    /** Returns the amount of the donation converted into the recipient's currency
     * @return the amount of the donation converted into the recipient's currency. Always more than zero*/
    public double getAmountInUserCurrency() { return amountInUserCurrency; }

    /** Returns the displayed name of the recipient
     * @return the displayed name of the recipient of the donation*/
    public String getRecipientName() { return recipientName; }

    /** Returns an object with full information about the recipient
     * @return an object with full information about the recipient {@link RecipientInfo}*/
    public RecipientInfo getRecipient() { return recipient; }

    /** Returns the date and time of donation creation
     * @return the date and time of donation creation in format<br>{@code yyyy-MM-dd HH:mm:ss}. See {@link java.time.format.DateTimeFormatter} for more information*/
    public String getCreatedAt() { return createdAt; }

    /** Returns the date and time when the donation was shown on the screen.
     * @return the date and time when the donation was shown on the screen.*/
    public String getShownAt() { return shownAt; }

    /** Returns the reason for the rejection or the donation status
     * @return the reason for the rejection or the donation status (e.g. {@code default})*/
    public String getReason() { return reason; }


    /**{@inheritDoc}
     * <p>Returns HandlerList with all handlers of this event</p>
     * @return HandlerList with all handlers*/
    @Override
    public @NonNull HandlerList getHandlers() {
        return handlers;
    }


    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * A record containing detailed information about the recipient of the donation.
     *
     * @param userId The recipient's unique identifier.
     * @param code The internal code of recipient.
     * @param name The recipient's display name.
     * @param avatar The URL of the recipient's avatar.
     * */
    public record RecipientInfo(long userId, String code, String name, String avatar) {
    }

    public static DonationEvent test(String username, String message, double amount, String currency) {
        return new DonationEvent(
                -1, null, username, message, null, null, amount, currency,
                -1, -1, null, null, null, null, null);
    }
}