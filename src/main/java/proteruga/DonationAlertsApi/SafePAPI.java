package proteruga.DonationAlertsApi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.jetbrains.annotations.NotNull;

public class SafePAPI {
    public static @NotNull String setPlaceholders(@NotNull String string) {
        return DonationAlertsApi.isPlaceholdersEnabled() ? PlaceholderAPI.setPlaceholders(null, string) : string;
    }
}
