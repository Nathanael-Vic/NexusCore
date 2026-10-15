package com.nexussmp.nexuscore.core.util;

import com.mojang.authlib.GameProfile;
import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

/**
 * O perfil de um jogador em NBT: nome, id e a textura, juntos.
 *
 * <p>Tudo aqui existe por um detalhe do jogo: o cliente <b>nao sabe</b> buscar a skin de alguem
 * a partir do id. O {@code SkinManager} so desembrulha a textura que ja vem dentro do perfil — sem
 * ela, o que aparece e o Steve. Por isso, quando o servidor desenha alguem que nao esta ali
 * (holograma, skinwalker), ele manda o perfil inteiro, que e a unica versao certa da historia:
 * ela ja passou por mods de skin como o SkinRestorer e vale tambem em servidor offline.
 *
 * <p>O formato e o mesmo que o jogo usa na cabeca de jogador ({@link ResolvableProfile}), entao nao
 * inventamos serializacao nenhuma.
 */
public final class Profiles {
    public static CompoundTag write(GameProfile profile) {
        return ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, new ResolvableProfile(profile))
                .resultOrPartial(error -> NexusCore.LOGGER.warn("[NexusCore] perfil nao coube em NBT: {}", error))
                .filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast)
                .orElseGet(CompoundTag::new);
    }

    /** {@code null} quando nao ha perfil guardado (ou o que havia nao e legivel). */
    @Nullable
    public static GameProfile read(CompoundTag tag) {
        if (tag.isEmpty()) {
            return null;
        }
        return ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> NexusCore.LOGGER.warn("[NexusCore] perfil ilegivel: {}", error))
                .map(ResolvableProfile::gameProfile)
                .orElse(null);
    }

    private Profiles() {}
}
