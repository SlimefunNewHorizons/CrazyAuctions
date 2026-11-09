package com.badbones69.crazyauctions.fees;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeePolicyTest {

    private final FeePolicy def = FeePolicy.fromConfig(new YamlConfiguration());

    @Test
    void valoresPorDefecto() {
        assertTrue(def.enabled);
        assertEquals(5.0, def.commissionPercent);
        assertEquals(50, def.listingMin);
        assertTrue(def.destroy);
    }

    @Test
    void tasaDePublicacionRespetaMinimo() {
        assertEquals(50, def.listingFee(100, false));      // 1 % = 1 -> minimo
        assertEquals(50, def.listingFee(5000, false));     // 1 % = 50
        assertEquals(120, def.listingFee(12000, false));   // 1 % = 120
    }

    @Test
    void tasaConTope() {
        FeePolicy p = new FeePolicy(true, 5, 1, 50, 100, true, "", true, "es");
        assertEquals(100, p.listingFee(1_000_000, false));
    }

    @Test
    void comisionSobrePrecioFinal() {
        assertEquals(50, def.commission(1000, false));
        assertEquals(950, def.netToSeller(1000, false));
        assertEquals(1, def.commission(10, false));        // 0,5 redondea arriba
        assertEquals(0, def.commission(0, false));
    }

    @Test
    void exencionYDesactivado() {
        assertEquals(0, def.listingFee(1000, true));
        assertEquals(0, def.commission(1000, true));
        FeePolicy off = new FeePolicy(false, 5, 1, 50, 0, true, "", true, "es");
        assertEquals(0, off.listingFee(1000, false));
        assertEquals(1000, off.netToSeller(1000, false));
    }

    @Test
    void nuncaNegativoNiMayorQueElPrecio() {
        FeePolicy p = new FeePolicy(true, 250, -3, -10, -5, true, "", true, "es");
        assertEquals(100, p.commissionPercent);
        assertEquals(0, p.listingFee(1000, false));
        assertEquals(1000, p.commission(1000, false));
        assertEquals(0, p.netToSeller(1000, false));
        assertEquals(0, def.listingFee(-5, false));
        assertEquals(0, def.commission(-5, false));
    }

    @Test
    void bancoSinNombreCaeADestruir() {
        YamlConfiguration c = new YamlConfiguration();
        c.set("Settings.Fees.Sink", "bank");
        assertTrue(FeePolicy.fromConfig(c).destroy);
        c.set("Settings.Fees.Bank-Player", "Banco");
        assertEquals(false, FeePolicy.fromConfig(c).destroy);
    }

    @Test
    void sinNaNNiDesbordamiento() {
        FeePolicy p = new FeePolicy(true, Double.NaN, 100, 0, 0, true, "", true, "en");
        assertEquals(0, p.commission(1000, false));
        assertEquals("en", p.lang);
        assertEquals(Long.MAX_VALUE, p.listingFee(Long.MAX_VALUE, false));
    }
}
