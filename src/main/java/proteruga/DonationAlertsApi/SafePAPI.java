package proteruga.DonationAlertsApi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class SafePAPI {
    public static @NotNull String setPlaceholders(OfflinePlayer player, @NotNull String string) {
        return DonationAlertsApi.isPlaceholdersEnabled() ? PlaceholderAPI.setPlaceholders(player, string) : string;
    }
}
