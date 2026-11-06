package com.badbones69.crazyauctions.currency;

import com.badbones69.crazyauctions.CrazyAuctions;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

public class VaultSupport {

    private final CrazyAuctions plugin = CrazyAuctions.get();

    private Economy vault = null;

    public Economy getVault() {
        return this.vault;
    }

    public boolean setupEconomy() {
        RegisteredServiceProvider<Economy> serviceProvider = this.plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (serviceProvider != null) this.vault = serviceProvider.getProvider();

        return this.vault != null;
    }

    public long getMoney(@NotNull Player player) {
        return (long) this.vault.getBalance(player);
    }

    public boolean removeMoney(@NotNull Player player, long amount) {
        EconomyResponse result = this.vault.withdrawPlayer(player, amount);
        return result.transactionSuccess();
    }

    public boolean removeMoney(@NotNull OfflinePlayer player, long amount) {
        EconomyResponse result = this.vault.withdrawPlayer(player, amount);
        return result.transactionSuccess();
    }

    /** Paga en la modalidad del mundo indicado, aunque el jugador este en otra o desconectado. */
    public boolean addMoney(OfflinePlayer player, String world, long amount) {
        return this.vault.depositPlayer(player, world, amount).transactionSuccess();
    }

    /** Cobra en la modalidad del mundo indicado, aunque el jugador este en otra o desconectado. */
    public boolean removeMoney(OfflinePlayer player, String world, long amount) {
        return this.vault.withdrawPlayer(player, world, amount).transactionSuccess();
    }

    public boolean addMoney(Player player, long amount) {
        EconomyResponse result = this.vault.depositPlayer(player, amount);
        return result.transactionSuccess();
    }

    public boolean addMoney(OfflinePlayer player, long amount) {
        EconomyResponse result = this.vault.depositPlayer(player, amount);
        return result.transactionSuccess();
    }
}