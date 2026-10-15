package com.nexussmp.nexuscore.radio;

import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.radio.network.RadioPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Recebe os cliques da tela e confere tudo de novo.
 *
 * <p>O que chega daqui e um pedido, nunca uma ordem: o cliente diz "quero ouvir a sessao X" e este
 * lado decide se a sessao X e mesmo a que esta no ar, e se quem pediu pode encerra-la.
 */
public final class RadioServerHandler {

    /** Abrir o ar pela tela: so quem e locutor consegue, e o titulo e conferido de novo. */
    public static void start(RadioPayloads.Start payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!CommonConfig.isModuleEnabled(ModuleIds.RADIO) || !RadioManager.mayBroadcast(player)) {
            RadioManager.sendState(player, RadioManager.live(), false);
            return;
        }
        if (RadioManager.start(player, payload.title(), payload.presenterName()) == null) {
            RadioManager.sendState(player, RadioManager.live(), false);
        }
    }

    public static void action(RadioPayloads.Action payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.kind()) {
            case LISTEN -> RadioManager.listen(player, payload.sessionId());
            case LEAVE -> RadioManager.leave(player);
            case END -> end(player, payload);
            case MIC_ON -> RadioManager.setMicMuted(player, false);
            case MIC_OFF -> RadioManager.setMicMuted(player, true);
        }
    }

    /** A mesa de som: tocar, pausar, parar e os volumes que valem para todo mundo. */
    public static void mix(RadioPayloads.Mix payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.kind()) {
            case MUSIC_PLAY -> RadioManager.playMusic(player, payload.url());
            case MUSIC_PAUSE -> RadioManager.pauseMusic(player, true);
            case MUSIC_RESUME -> RadioManager.pauseMusic(player, false);
            case MUSIC_STOP -> RadioManager.stopMusic(player);
            case MUSIC_VOLUME -> RadioManager.setMusicVolume(player, payload.value());
            case VOICE_VOLUME -> payload.target()
                    .ifPresent(voice -> RadioManager.setVoiceVolume(player, voice, payload.value()));
        }
    }

    private static void end(ServerPlayer player, RadioPayloads.Action payload) {
        BroadcastSession session = RadioManager.live();
        if (session == null || !session.id().equals(payload.sessionId())) {
            RadioManager.sendState(player, session, false);
            return;
        }
        if (!session.presenterId().equals(player.getUUID())
                && !player.hasPermissions(RadioManager.MODERATOR_LEVEL)) {
            // Nao e dono nem moderador: o estado volta como esta, e o botao some da tela dele.
            RadioManager.sendState(player, session, false);
            return;
        }
        RadioManager.stop(player.server);
    }

    private RadioServerHandler() {}
}
