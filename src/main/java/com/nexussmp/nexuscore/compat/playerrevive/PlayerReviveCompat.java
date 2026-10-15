package com.nexussmp.nexuscore.compat.playerrevive;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import team.creative.playerrevive.api.IBleeding;

/**
 * Saber se alguem esta caido pelo PlayerRevive.
 *
 * <p>Nas backrooms nao existe estado de abatido: ali ninguem toma dano, e se a morte chegar por
 * algum caminho ela vira uma saida pela porta. O problema seria a pessoa <b>chegar</b> ja caida —
 * puxada pelo evento raro ou mandada pelo comando enquanto sangra no mundo. Caida ela nao anda, nao
 * abre porta e nao tem como sair: viraria um castigo, nao um susto.
 *
 * <p>O PlayerRevive e opcional. A checagem de {@link ModList} e o que garante que a classe interna —
 * a unica que encosta nas classes dele — so seja carregada quando o mod estiver la.
 */
public final class PlayerReviveCompat {
    private static final String MOD_ID = "playerrevive";

    private static Boolean loaded;
    /** Uma falha basta para desligar a checagem: melhor sem ela do que um erro por tick. */
    private static boolean broken;

    /** {@code true} so quando o PlayerRevive esta instalado e a pessoa esta caida agora. */
    public static boolean isDowned(Player player) {
        if (broken || !loaded()) {
            return false;
        }
        try {
            return Bleeding.isDowned(player);
        } catch (Throwable error) {
            broken = true;
            NexusCore.LOGGER.warn("[NexusCore] nao consegui ler o estado do PlayerRevive; seguindo sem ele", error);
            return false;
        }
    }

    private static boolean loaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded(MOD_ID);
        }
        return loaded;
    }

    /**
     * O estado fica num attachment do NeoForge ({@code playerrevive:bleeding}), entao da para pegar
     * pelo id, sem depender de nenhuma classe estatica do mod. So a interface publica da API dele e
     * usada aqui.
     */
    private static final class Bleeding {
        private static AttachmentType<?> type;

        static boolean isDowned(Player player) {
            AttachmentType<?> attachment = type();
            return attachment != null
                    && player.getExistingDataOrNull(attachment) instanceof IBleeding bleeding
                    && bleeding.isBleeding();
        }

        private static AttachmentType<?> type() {
            if (type == null) {
                type = NeoForgeRegistries.ATTACHMENT_TYPES
                        .get(ResourceLocation.fromNamespaceAndPath(MOD_ID, "bleeding"));
            }
            return type;
        }
    }

    private PlayerReviveCompat() {}
}
