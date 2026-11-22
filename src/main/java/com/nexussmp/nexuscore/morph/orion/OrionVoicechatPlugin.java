package com.nexussmp.nexuscore.morph.orion;

import com.nexussmp.nexuscore.NexusCore;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * O ORION nao fala: quem esta na forma dele tem o microfone cortado no servidor, antes de o som
 * chegar a qualquer pessoa (perto, cochicho ou grupo, tanto faz).
 *
 * <p>So o Simple Voice Chat carrega esta classe, e so se estiver instalado. Plugin separado dos
 * outros do mod (papagaio, backrooms, radio): cortar aqui nao muda o que eles escutam, porque o
 * pacote cancelado nao vai para ninguem.
 */
@ForgeVoicechatPlugin
public final class OrionVoicechatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return NexusCore.MOD_ID + "_orion";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // Prioridade alta: corta antes de os outros plugins (papagaio, skinwalker) guardarem a fala.
        registration.registerEvent(MicrophonePacketEvent.class, OrionVoicechatPlugin::onMicrophone, 100);
    }

    private static void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || !OrionMorph.muted(sender.getPlayer().getUuid())) {
            return;
        }
        event.cancel();
        if (sender.getPlayer().getPlayer() instanceof ServerPlayer player) {
            player.server.execute(() -> OrionMorph.warn(player));
        }
    }
}
