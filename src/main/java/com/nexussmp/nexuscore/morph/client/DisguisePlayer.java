package com.nexussmp.nexuscore.morph.client;

import com.mojang.authlib.GameProfile;
import com.nexussmp.nexuscore.core.client.PlayerSkins;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.PlayerSkin;

import java.util.UUID;

/**
 * O jogador de mentira que se desenha no lugar de quem copiou outro jogador.
 *
 * <p>Nunca entra no mundo. Tem o perfil de quem foi copiado — por isso o nome em cima da cabeca e o
 * dele —, e a skin sai do {@link PlayerSkins}: a da pessoa online, a textura que veio guardada no
 * anel, ou a buscada uma vez. As partes de fora da skin (chapeu, jaqueta, mangas) aparecem todas;
 * de outro jeito, o jogo trataria o boneco como alguem que desligou todas elas.
 */
final class DisguisePlayer extends RemotePlayer {
    /** Todas as partes de fora da skin ligadas. */
    private static final byte ALL_MODEL_PARTS = 0x7F;

    /** Quem esta usando este disfarce — nao quem foi copiado. */
    private final UUID wearer;

    DisguisePlayer(ClientLevel level, GameProfile copied, UUID wearer) {
        super(level, copied);
        this.wearer = wearer;
        entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, ALL_MODEL_PARTS);
        setSilent(true);
    }

    UUID wearer() {
        return wearer;
    }

    @Override
    public PlayerSkin getSkin() {
        return PlayerSkins.of(getGameProfile(), getUUID());
    }
}
