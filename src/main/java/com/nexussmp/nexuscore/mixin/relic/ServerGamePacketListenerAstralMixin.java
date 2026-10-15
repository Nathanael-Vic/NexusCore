package com.nexussmp.nexuscore.mixin.relic;

import com.nexussmp.nexuscore.relic.astral.AstralProjection;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * O menu de espectador (teclas de numero) leva para perto de qualquer jogador, em qualquer mundo.
 * Fora do corpo pela mascara, nao: a alma esta presa ao corpo por um fio de poucos blocos.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerAstralMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleTeleportToEntityPacket", at = @At("HEAD"), cancellable = true)
    private void nexus$astralStaysTethered(ServerboundTeleportToEntityPacket packet, CallbackInfo callback) {
        if (AstralProjection.isProjecting(player)) {
            callback.cancel();
        }
    }
}
