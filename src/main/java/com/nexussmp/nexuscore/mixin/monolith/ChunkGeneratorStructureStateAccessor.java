package com.nexussmp.nexuscore.mixin.monolith;

import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A fonte de biomas do mundo, para o monolito procurar o bioma dele (o jogo nao a expoe). */
@Mixin(ChunkGeneratorStructureState.class)
public interface ChunkGeneratorStructureStateAccessor {

    @Accessor("biomeSource")
    BiomeSource nexus$biomeSource();
}
