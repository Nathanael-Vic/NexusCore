package com.nexussmp.nexuscore.space;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A dimensao Space (a estacao espacial / lobby).
 *
 * <p>Ela existe por dados: {@code data/nexuscore/dimension/space.json} e companhia. Como o mod e um
 * datapack embutido, a dimensao esta sempre ativa, sem ninguem precisar instalar nada. Em mundo que ja
 * existe, ela aparece no proximo boot depois de instalar a versao do mod que a contem (nao com /reload).
 *
 * <p>Nesta etapa a dimensao so existe e tem o ceu certo. Nascer nela, portal e quartos sao a etapa 4B,
 * que e codigo, nao dados.
 */
public final class SpaceDimensions {
    public static final ResourceLocation SPACE_ID = NexusCore.id("space");
    public static final ResourceKey<Level> SPACE = ResourceKey.create(Registries.DIMENSION, SPACE_ID);

    /** Cor unica do espaco: ceu, nevoa e bioma usam esta mesma constante. */
    public static final int SKY_COLOR = 0x020308;

    private SpaceDimensions() {}
}
