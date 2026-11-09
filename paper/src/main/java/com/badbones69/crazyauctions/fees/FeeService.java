package com.badbones69.crazyauctions.fees;

import com.badbones69.crazyauctions.CrazyAuctions;
import com.badbones69.crazyauctions.api.enums.Files;
import com.badbones69.crazyauctions.currency.VaultSupport;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cobros de tasa/comision sobre Vault, con destino (destruir/banco), registro y confirmacion. */
public final class FeeService {

    public static final String EXEMPT_PERMISSION = "crazyauctions.fee.exempt";

    /** Monitor unico: una compra/cobro a la vez, evita dobles cobros. */
    public static final Object LOCK = new Object();

    private static final long CONFIRM_MS = 30_000L;

    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    private record Pending(String key, long expires) {}

    public FeePolicy policy() {
        return FeePolicy.fromConfig(Files.config.getConfiguration());
    }

    public static boolean isExempt(Player p) {
        return p != null && p.hasPermission(EXEMPT_PERMISSION);
    }

    /** True si ya habia una orden identica pendiente y vigente (y la consume). */
    public boolean consumeConfirmation(UUID id, String key) {
        Pending p = pending.get(id);
        if (p != null && p.key().equals(key) && p.expires() > System.currentTimeMillis()) {
            pending.remove(id);
            return true;
        }
        pending.put(id, new Pending(key, System.currentTimeMillis() + CONFIRM_MS));
        return false;
    }

    /** Cobra {@code fee} al jugador. false = no se pudo (saldo, error) y no se cobro nada. */
    public boolean charge(Player player, long fee) {
        if (fee <= 0) return true;
        VaultSupport s = CrazyAuctions.get().getSupport();
        if (s.getVault() == null || s.getMoney(player) < fee) return false;
        return s.removeMoney(player, fee);
    }

    public void refund(Player player, long fee) {
        if (fee > 0) CrazyAuctions.get().getSupport().addMoney(player, fee);
    }

    /** Destino de lo cobrado: destruido o ingresado al banco configurado. */
    public void sink(FeePolicy pol, String world, long amount) {
        if (amount <= 0 || pol.destroy) return;
        OfflinePlayer bank = CrazyAuctions.get().getServer().getOfflinePlayer(pol.bankPlayer);
        if (!CrazyAuctions.get().getSupport().addMoney(bank, world, amount)) {
            CrazyAuctions.get().getLogger().warning("Fee: no se pudo ingresar " + amount + " al banco " + pol.bankPlayer + " (destruido)");
        }
    }

    public void log(String type, String who, long amount, long price, String world, String sink) {
        String line = Instant.now() + " " + type + " player=" + who + " amount=" + amount + " price=" + price
                + " world=" + world + " sink=" + sink + System.lineSeparator();
        CrazyAuctions.get().getLogger().info("Fee " + line.trim());
        try {
            File f = new File(CrazyAuctions.get().getDataFolder(), "fees.log");
            java.nio.file.Files.writeString(f.toPath(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            CrazyAuctions.get().getLogger().warning("No se pudo escribir fees.log: " + e.getMessage());
        }
    }

    public String sinkName(FeePolicy pol) {
        return pol.destroy ? "destroy" : "bank:" + pol.bankPlayer;
    }

    /**
     * Liquida una venta ya cobrada al comprador: el vendedor recibe el precio menos la comision.
     * @return neto pagado al vendedor, o -1 si el deposito fallo (el llamador debe devolver el dinero)
     */
    public long settle(OfflinePlayer seller, Player sellerOnline, boolean flaggedExempt, String world, long price, String sellerName) {
        FeePolicy pol = policy();
        long fee = pol.commission(price, flaggedExempt || isExempt(sellerOnline));
        long net = price - fee;
        if (net > 0 && !CrazyAuctions.get().getSupport().addMoney(seller, world, net)) return -1;
        if (fee > 0) {
            sink(pol, world, fee);
            log("commission", sellerName, fee, price, world, sinkName(pol));
        }
        return net;
    }
}
