package com.badbones69.crazyauctions.fees;

import com.badbones69.crazyauctions.Methods;
import com.badbones69.crazyauctions.api.enums.Files;

import java.util.Map;

/** Mensajes ES/EN de tasas y comisiones. messages.yml (Messages.Fees.<clave>) los puede sobreescribir. */
public final class FeeText {

    private static final Map<String, String[]> TEXT = Map.of(
            "listing-confirm", new String[]{
                    "&eListar cuesta una tasa de publicacion de &a%fee% &e(no reembolsable). Si se vende, la comision es del &a%pct%%&e: recibiras &a%net%&e. Repite el mismo comando en 30 s para confirmar.",
                    "&eListing costs a publication fee of &a%fee% &e(non-refundable). If it sells, a &a%pct%%&e commission applies: you will receive &a%net%&e. Repeat the same command within 30 s to confirm."},
            "listing-charged", new String[]{
                    "&7Tasa de publicacion cobrada: &a%fee%&7.",
                    "&7Publication fee charged: &a%fee%&7."},
            "listing-need-money", new String[]{
                    "&cNo tienes dinero para la tasa de publicacion (&a%fee%&c).",
                    "&cYou cannot afford the publication fee (&a%fee%&c)."},
            "listing-failed", new String[]{
                    "&cNo se pudo publicar; no se cobro nada.",
                    "&cListing failed; you were not charged."},
            "sold-net", new String[]{
                    "&7Comision de venta (&a%pct%%&7): &c-%fee%&7. Recibes &a%net%&7.",
                    "&7Sales commission (&a%pct%%&7): &c-%fee%&7. You receive &a%net%&7."},
            "sale-failed", new String[]{
                    "&cLa venta fallo; se te devolvio el dinero.",
                    "&cThe sale failed; your money was returned."});

    public static String get(FeePolicy pol, String key, Map<String, String> ph) {
        String[] def = TEXT.get(key);
        String msg = Files.messages.getConfiguration().getString("Messages.Fees." + key);
        if (msg == null) msg = def[pol.lang.equals("en") ? 1 : 0];
        for (Map.Entry<String, String> e : ph.entrySet()) msg = msg.replace(e.getKey(), e.getValue());
        return Methods.getPrefix(msg);
    }
}
