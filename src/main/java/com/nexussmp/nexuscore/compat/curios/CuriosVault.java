package com.nexussmp.nexuscore.compat.curios;

import com.nexussmp.nexuscore.core.vault.PlayerVault;
import com.nexussmp.nexuscore.core.vault.VaultCompartment;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Os acessorios do Curios entram no guarda-volumes junto com o inventario.
 *
 * <p>Sem isto, quem cai nas backrooms perde as maos mas continua de colar, anel e capa — o efeito
 * de chegar la sem nada quebra, e os bonus dos acessorios continuariam valendo la dentro.
 *
 * <p>O Curios e opcional: nada aqui carrega sem ele instalado. {@link #install()} e chamado sempre,
 * e so entra neste pacote se o mod estiver presente — por isso a classe do compartimento e uma
 * classe a parte, tocada so depois da checagem.
 */
public final class CuriosVault {
    private static final String MOD_ID = "curios";

    /** Registra o compartimento quando o Curios estiver instalado. */
    public static void install() {
        if (ModList.get().isLoaded(MOD_ID)) {
            PlayerVault.register(new CuriosCompartment());
        }
    }

    /**
     * O proprio Curios ja sabe fazer isto: e o mesmo caminho que ele usa quando alguem morre e
     * renasce. {@code saveInventory(true)} copia as vagas e as esvazia de uma vez; o cliente pega a
     * mudanca no tick seguinte, pelo sincronismo normal do mod.
     */
    private static final class CuriosCompartment implements VaultCompartment {
        @Override
        public String id() {
            return MOD_ID;
        }

        @Override
        @Nullable
        public Tag stash(ServerPlayer player) {
            return CuriosApi.getCuriosInventory(player)
                    .map(inventory -> inventory.saveInventory(true))
                    .filter(saved -> !saved.isEmpty())
                    .orElse(null);
        }

        @Override
        public void restore(ServerPlayer player, Tag saved) {
            if (saved instanceof ListTag list) {
                CuriosApi.getCuriosInventory(player).ifPresent(inventory -> inventory.loadInventory(list));
            }
        }
    }

    private CuriosVault() {}
}
