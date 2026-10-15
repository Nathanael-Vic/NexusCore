package com.nexussmp.nexuscore.compat.jade;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.relic.stone.UnyieldingStoneBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * "Dono: Fulano" na plaquinha da pedra inabalavel — so para admin.
 *
 * <p>A decisao e do servidor: e ele que monta os dados da plaquinha, e so poe o nome quando quem
 * pergunta e operador. Para todo o resto, o nome nunca sai do servidor — nao ha o que esconder no
 * cliente, porque o cliente nao recebe.
 */
final class StoneOwnerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    static final StoneOwnerProvider INSTANCE = new StoneOwnerProvider();
    private static final ResourceLocation UID = NexusCore.id("stone_owner");
    private static final String OWNER = "owner";
    private static final int ADMIN_LEVEL = 2;

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getPlayer().hasPermissions(ADMIN_LEVEL)
                && accessor.getBlockEntity() instanceof UnyieldingStoneBlockEntity stone && !stone.ownerName().isEmpty()) {
            data.putString(OWNER, stone.ownerName());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(OWNER)) {
            tooltip.add(Component.translatable("nexuscore.relic.unyielding_stone.owner", data.getString(OWNER))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private StoneOwnerProvider() {}
}
