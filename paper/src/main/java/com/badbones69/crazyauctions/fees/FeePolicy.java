package com.badbones69.crazyauctions.fees;

import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculo puro de la tasa de publicacion y la comision de venta (sin Bukkit en la logica).
 * Todos los importes son enteros no negativos.
 */
public final class FeePolicy {

    public final boolean enabled;
    public final double commissionPercent;
    public final double listingPercent;
    public final long listingMin;
    public final long listingMax; // 0 = sin tope
    public final boolean destroy;
    public final String bankPlayer;
    public final boolean confirmListing;
    public final String lang;

    public FeePolicy(boolean enabled, double commissionPercent, double listingPercent, long listingMin,
                     long listingMax, boolean destroy, String bankPlayer, boolean confirmListing, String lang) {
        this.enabled = enabled;
        this.commissionPercent = clampPercent(commissionPercent);
        this.listingPercent = clampPercent(listingPercent);
        this.listingMin = Math.max(0, listingMin);
        this.listingMax = Math.max(0, listingMax);
        this.destroy = destroy;
        this.bankPlayer = bankPlayer == null ? "" : bankPlayer.trim();
        this.confirmListing = confirmListing;
        this.lang = "en".equalsIgnoreCase(lang) ? "en" : "es";
    }

    /** Lee {@code Settings.Fees}; las claves ausentes toman los valores por defecto. */
    public static FeePolicy fromConfig(ConfigurationSection root) {
        ConfigurationSection s = root == null ? null : root.getConfigurationSection("Settings.Fees");
        if (s == null) s = new org.bukkit.configuration.MemoryConfiguration();
        String sink = s.getString("Sink", "destroy");
        String bank = s.getString("Bank-Player", "");
        boolean destroy = !"bank".equalsIgnoreCase(sink) || bank == null || bank.isBlank();
        return new FeePolicy(
                s.getBoolean("Enabled", true),
                s.getDouble("Commission-Percent", 5.0),
                s.getDouble("Listing-Percent", 1.0),
                s.getLong("Listing-Min", 50),
                s.getLong("Listing-Max", 0),
                destroy, bank,
                s.getBoolean("Confirm-Listing", true),
                s.getString("Language", "es"));
    }

    private static double clampPercent(double p) {
        if (Double.isNaN(p) || p < 0) return 0;
        return Math.min(p, 100);
    }

    private static long percentOf(long amount, double percent) {
        if (amount <= 0 || percent <= 0) return 0;
        return BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValueExact();
    }

    /** Tasa que se cobra al listar (no reembolsable). */
    public long listingFee(long price, boolean exempt) {
        if (!enabled || exempt || price < 0) return 0;
        if (listingPercent <= 0 && listingMin <= 0) return 0;
        long fee = Math.max(listingMin, percentOf(price, listingPercent));
        if (listingMax > 0) fee = Math.min(fee, Math.max(listingMax, 0));
        return fee;
    }

    /** Comision sobre el precio final; nunca supera el precio. */
    public long commission(long finalPrice, boolean exempt) {
        if (!enabled || exempt || finalPrice <= 0) return 0;
        return Math.min(finalPrice, percentOf(finalPrice, commissionPercent));
    }

    /** Lo que recibe el vendedor. */
    public long netToSeller(long finalPrice, boolean exempt) {
        return finalPrice - commission(finalPrice, exempt);
    }
}
