package com.badbones69.crazyauctions.market;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Resolves a Minecraft world to one isolated auction market. */
public final class MarketResolver {

    private final String legacyMarket;
    private final Map<String, String> worldMarkets;
    private final Map<String, String> labels;
    private final Set<String> deniedWorlds;

    public MarketResolver(
            @NotNull String legacyMarket,
            @NotNull Map<String, List<String>> marketWorlds,
            @NotNull Map<String, String> labels,
            @NotNull Set<String> deniedWorlds
    ) {
        this.legacyMarket = normalize(legacyMarket);
        this.worldMarkets = new LinkedHashMap<>();
        this.labels = labels.entrySet().stream().collect(Collectors.toMap(
                entry -> normalize(entry.getKey()),
                Map.Entry::getValue,
                (left, right) -> right,
                LinkedHashMap::new
        ));
        this.deniedWorlds = deniedWorlds.stream().map(MarketResolver::normalize).collect(Collectors.toUnmodifiableSet());

        marketWorlds.forEach((market, worlds) -> {
            String normalizedMarket = normalize(market);
            for (String world : worlds) {
                this.worldMarkets.put(normalize(world), normalizedMarket);
            }
        });
    }

    /** Returns the market for a world, or empty when auctions are disabled there. */
    public Optional<String> resolve(@NotNull String worldName) {
        String world = normalize(worldName);
        if (this.deniedWorlds.contains(world)) {
            return Optional.empty();
        }
        String direct = this.worldMarkets.get(world);
        if (direct != null) {
            return Optional.of(direct);
        }
        for (Map.Entry<String, String> entry : this.worldMarkets.entrySet()) {
            String mappedWorld = entry.getKey();
            String market = entry.getValue();
            if (world.startsWith(mappedWorld) || mappedWorld.startsWith(world) || world.contains(market)) {
                return Optional.of(market);
            }
        }
        return Optional.empty();
    }

    /** Compares a world against a listing, assigning legacy entries to the configured market. */
    public boolean matches(@NotNull String worldName, String listingMarket) {
        String market = listingMarket == null || listingMarket.isBlank()
                ? this.legacyMarket
                : normalize(listingMarket);
        return resolve(worldName).map(market::equals).orElse(false);
    }

    /** Un mundo del mercado, para mover dinero en su modalidad aunque las partes esten fuera. */
    public Optional<String> world(@NotNull String market) {
        String normalized = normalize(market);
        return this.worldMarkets.entrySet().stream()
                .filter(entry -> entry.getValue().equals(normalized))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public String legacyMarket() {
        return this.legacyMarket;
    }

    public String label(@NotNull String market) {
        String normalized = normalize(market);
        return this.labels.getOrDefault(normalized, normalized);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
