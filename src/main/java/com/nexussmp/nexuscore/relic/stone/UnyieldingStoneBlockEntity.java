package com.nexussmp.nexuscore.relic.stone;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * De quem e a pedra. So o servidor sabe: nada daqui vai para o cliente — nem o id, nem o nome —,
 * entao nenhum mod no cliente de ninguem descobre quem a colocou. O unico que ve e o admin, pelo
 * Jade, e so porque o servidor manda para ele ({@code NexusJadePlugin}).
 */
public final class UnyieldingStoneBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String ownerName = "";

    public UnyieldingStoneBlockEntity(BlockPos pos, BlockState state) {
        super(NexusRegistries.UNYIELDING_STONE_ENTITY.get(), pos, state);
    }

    void bind(UUID owner, String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
        setChanged();
    }

    public boolean isOwner(UUID player) {
        return player.equals(owner);
    }

    /** O nome de quem colocou, como era na hora; vazio para pedra sem dono (posta por comando). */
    public String ownerName() {
        return ownerName;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("owner", owner);
            tag.putString("ownerName", ownerName);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        ownerName = tag.getString("ownerName");
    }
}
