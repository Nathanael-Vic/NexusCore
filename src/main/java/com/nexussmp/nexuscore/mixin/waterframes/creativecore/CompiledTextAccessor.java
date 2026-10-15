package com.nexussmp.nexuscore.mixin.waterframes.creativecore;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import team.creative.creativecore.client.render.text.CompiledText;

/** O CompiledText tem setShadow mas nao tem getter; precisamos ler para restaurar depois. */
@Mixin(value = CompiledText.class, remap = false)
public interface CompiledTextAccessor {

    @Accessor("shadow")
    boolean nexus$hasShadow();
}
