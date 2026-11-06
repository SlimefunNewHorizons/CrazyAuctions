package com.badbones69.crazyauctions.market;

import com.badbones69.crazyauctions.CrazyAuctions;
import com.badbones69.crazyauctions.Methods;
import com.badbones69.crazyauctions.api.enums.Files;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Owns market configuration, legacy migration and runtime access checks. */
public final class AuctionMarketManager {

    private static final String ITEMS = "Items";
    private static final String EXPIRED = "OutOfTime/Cancelled";

    private final CrazyAuctions plugin;
    private boolean enabled;
    private MarketResolver resolver;

    public AuctionMarketManager(@NotNull CrazyAuctions plugin) {
        this.plugin = plugin;
    }

    /** Reloads market definitions and stamps legacy listings without changing ownership. */
    public void reload() {
        FileConfiguration config = Files.config.getConfiguration();
        this.enabled = config.getBoolean("Settings.Markets.Enabled", true);
        String legacy = config.getString("Settings.Markets.Legacy-Market", "survival");
        Map<String, List<String>> worlds = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        ConfigurationSection definitions = config.getConfigurationSection("Settings.Markets.Definitions");

        if (definitions != null) {
            for (String market : definitions.getKeys(false)) {
                worlds.put(market, definitions.getStringList(market + ".Worlds"));
                labels.put(market, definitions.getString(market + ".Label", market));
            }
        }

        Set<String> denied = new LinkedHashSet<>(config.getStringList("Settings.Markets.Denied-Worlds"));
        this.resolver = new MarketResolver(legacy, worlds, labels, denied);
        int migrated = migrateSection(ITEMS) + migrateSection(EXPIRED);
        if (migrated > 0) {
            Files.data.save();
        }

        this.plugin.getLogger().info("[Markets] " + worlds.size() + " modalidades cargadas; "
                + migrated + " publicaciones legacy migradas a '" + this.resolver.legacyMarket() + "'.");
    }

    /** Rejects auction activity in technical or unmapped worlds. */
    public boolean ensureAccessible(@NotNull Player player) {
        if (!this.enabled || market(player).isPresent()) {
            return true;
        }

        String message = Files.config.getConfiguration().getString(
                "Settings.Markets.Messages.Unavailable",
                "&cLas subastas no estan disponibles en este mundo."
        );
        player.sendMessage(Methods.getPrefix(message));
        return false;
    }

    public Optional<String> market(@NotNull Player player) {
        if (!this.enabled) {
            return Optional.of("global");
        }
        return this.resolver.resolve(player.getWorld().getName());
    }

    public String marketOrLegacy(@NotNull Player player) {
        return market(player).orElse(this.resolver.legacyMarket());
    }

    public void stamp(@NotNull FileConfiguration data, @NotNull String path, @NotNull Player player) {
        data.set(path + ".Market", marketOrLegacy(player));
    }

    public void copyMarket(@NotNull FileConfiguration data, @NotNull String from, @NotNull String to) {
        data.set(to + ".Market", listingMarket(data, from));
    }

    /** Mundo de referencia del mercado de una publicacion. */
    public String world(@NotNull FileConfiguration data, @NotNull String path) {
        String market = data.getString(path + ".Market", this.resolver.legacyMarket());
        return this.resolver.world(market).orElse("world");
    }

    public boolean matches(@NotNull Player player, @NotNull FileConfiguration data, @NotNull String path) {
        if (!this.enabled) {
            return true;
        }
        return this.resolver.matches(player.getWorld().getName(), data.getString(path + ".Market"));
    }

    /** Revalidates market membership immediately before any economic mutation. */
    public boolean ensureListingAccessible(@NotNull Player player, @NotNull FileConfiguration data, @NotNull String path) {
        if (data.contains(path) && matches(player, data, path)) {
            return true;
        }

        String message = Files.config.getConfiguration().getString(
                "Settings.Markets.Messages.Mismatch",
                "&cEsa publicacion pertenece a otra modalidad."
        );
        player.sendMessage(Methods.getPrefix(message));
        return false;
    }

    public String title(@NotNull Player player, @NotNull String base) {
        if (!this.enabled) {
            return base.replace("%market%", "Global");
        }
        return market(player)
                .map(market -> {
                    String label = this.resolver.label(market);
                    return base.contains("%market%") ? base.replace("%market%", label) : base + " &8| &b" + label;
                })
                .orElse(base.replace("%market%", "No disponible"));
    }

    private String listingMarket(FileConfiguration data, String path) {
        return data.getString(path + ".Market", this.resolver.legacyMarket());
    }

    private int migrateSection(String root) {
        FileConfiguration data = Files.data.getConfiguration();
        ConfigurationSection section = data.getConfigurationSection(root);
        if (section == null) {
            return 0;
        }

        int migrated = 0;
        for (String key : section.getKeys(false)) {
            String path = root + "." + key;
            if (!data.contains(path + ".Market")) {
                data.set(path + ".Market", this.resolver.legacyMarket());
                migrated++;
            }
        }
        return migrated;
    }
}
