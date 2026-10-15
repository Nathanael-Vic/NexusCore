package com.nexussmp.nexuscore.compat.envelope;

import com.nexussmp.nexuscore.core.config.ServerConfig;
import com.nexussmp.nexuscore.vampire.VampireLords;
import com.nexussmp.nexuscore.vampire.Vampires;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * O que marca um pombo carteiro do NexusCore, e de quem e a carta que ele leva.
 *
 * <p>As marcas sao marcas de entidade do proprio jogo, e o resto vai nos dados persistentes do
 * NeoForge. As duas coisas viajam no NBT do pombo — inclusive quando ele some no ar e o Envelope o
 * guarda como dados ate reaparecer perto de quem recebe, porque o Envelope guarda o NBT inteiro
 * menos uma lista de campos, e estes nao estao nela.
 */
final class Couriers {
    /** Veio de longe, chamado pela caixa de correio. */
    static final String SUMMONED = "nexuscore_summoned_courier";
    /** Terminou o trabalho e esta indo embora. */
    static final String LEAVING = "nexuscore_courier_leaving";
    /** Leva carta de lorde: aparece e soa como morcego. */
    static final String BAT = "nexuscore_bat_courier";

    private static final String DATA = "nexuscore:courier";
    private static final String MAILBOX = "Mailbox";
    private static final String OWNER = "Owner";

    /** Marca um pombo chamado para buscar a carta daquela caixa. */
    static void summoned(Pigeon pigeon, BlockPos mailbox, @Nullable UUID owner) {
        CompoundTag data = new CompoundTag();
        data.putLong(MAILBOX, mailbox.asLong());
        if (owner != null) {
            data.putUUID(OWNER, owner);
        }
        pigeon.getPersistentData().put(DATA, data);
        pigeon.addTag(SUMMONED);
    }

    static boolean isSummoned(Pigeon pigeon) {
        return pigeon.getTags().contains(SUMMONED);
    }

    static boolean isLeaving(Pigeon pigeon) {
        return pigeon.getTags().contains(LEAVING);
    }

    /** A caixa que este pombo veio buscar, enquanto ainda nao buscou. */
    @Nullable
    static BlockPos mailboxOf(Pigeon pigeon) {
        CompoundTag data = pigeon.getPersistentData().getCompound(DATA);
        return data.contains(MAILBOX) ? BlockPos.of(data.getLong(MAILBOX)) : null;
    }

    /** Buscou (ou desistiu): nao vai mais para caixa nenhuma. Quem e o dono da carta continua guardado. */
    static void clearMailbox(Pigeon pigeon) {
        pigeon.getPersistentData().getCompound(DATA).remove(MAILBOX);
    }

    /**
     * De quem e a carta que este pombo leva: o dono da caixa de onde ela saiu. Durante a entrega, o
     * Envelope sabe; antes de buscar e depois de terminar, sabe a marca que o NexusCore deixou.
     */
    @Nullable
    static UUID ownerOf(Pigeon pigeon) {
        Optional<UUID> delivering = pigeon.getCurrentDelivery().flatMap(Delivery::getOwner);
        if (delivering.isPresent()) {
            return delivering.get();
        }
        CompoundTag data = pigeon.getPersistentData().getCompound(DATA);
        return data.hasUUID(OWNER) ? data.getUUID(OWNER) : null;
    }

    /** Se a carta e de um lorde vampiro — e, portanto, vai de morcego. */
    static boolean isLord(@Nullable MinecraftServer server, @Nullable UUID owner) {
        return server != null && owner != null && Vampires.installed()
                && ServerConfig.get(ServerConfig.ENVELOPE_LORD_BATS)
                && VampireLords.get(server).isLord(owner);
    }

    private Couriers() {}
}
