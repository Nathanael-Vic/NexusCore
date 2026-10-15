package com.nexussmp.nexuscore.relic.refuge;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * O que a porta sabe: de quem e a sala do outro lado e ate quando ela fica aberta.
 *
 * <p>A porta que a chave abre tem dono e prazo: some sozinha quando o prazo passa — inclusive se o
 * chunk descarregou no meio e so carregou de novo dias depois. A porta de saida, la dentro, nao tem
 * nenhum dos dois: e parte da sala.
 */
public final class RefugeDoorBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    /** Em tempo de jogo; -1 para a porta que nunca some. */
    private long closesAt = -1;

    public RefugeDoorBlockEntity(BlockPos pos, BlockState state) {
        super(NexusRegistries.REFUGE_DOOR_ENTITY.get(), pos, state);
    }

    void open(UUID owner, long closesAt) {
        this.owner = owner;
        this.closesAt = closesAt;
        setChanged();
    }

    /** De quem e a sala do outro lado; {@code null} na porta de saida. */
    @Nullable
    UUID owner() {
        return owner;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, RefugeDoorBlockEntity door) {
        if (door.closesAt >= 0 && level.getGameTime() >= door.closesAt) {
            Refuges.closeDoor(level, pos);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putLong("closesAt", closesAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        closesAt = tag.contains("closesAt") ? tag.getLong("closesAt") : -1;
    }
}
