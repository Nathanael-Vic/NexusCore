package com.nexussmp.nexuscore.backrooms.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nexussmp.nexuscore.backrooms.BackroomsDimensions;
import com.nexussmp.nexuscore.backrooms.network.SkinwalkerPayload;
import com.nexussmp.nexuscore.core.client.ScreenNoise;
import com.nexussmp.nexuscore.core.registry.NexusSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * O susto visto de dentro: o que a tela da vitima faz enquanto aquilo acontece.
 *
 * <p>Tudo aqui e barato de proposito — vinheta, escuro e um tremor na camera. Nada de shader nem de
 * textura nova: susto bom e questao de tempo e de som, nao de efeito caro.
 */
public final class SkinwalkerEffects {
    private static final ResourceLocation VIGNETTE = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");
    /** Quanto a camera balanca no auge da revelacao, em blocos. */
    private static final double MAX_SHAKE = 0.12;

    // ---- a tela de quando ele pega ----
    /** O golpe: um clarao vermelho de tres ticks por cima do preto, e a camera sacode junto. */
    private static final int CAUGHT_IMPACT_TICKS = 3;
    /** Quando o "voce foi pego" comeca a aparecer. */
    private static final int CAUGHT_TITLE_AT = 14;
    /** O servidor devolve a pessoa ao mundo neste tick ({@code BackroomsManager.CATCH_TICKS}). */
    private static final int CAUGHT_RETURN_AT = 75;
    /** Antes da volta, chiado e titulo se apagam e sobra so o preto — o mesmo de onde a volta abre. */
    private static final int CAUGHT_FADE_TICKS = 16;

    @Nullable
    private static SkinwalkerPayload.Stage stage;
    private static int duration;
    private static int ticksLeft;

    public static void start(SkinwalkerPayload payload) {
        if (payload.stage() == SkinwalkerPayload.Stage.CLEAR) {
            clear();
            return;
        }
        stage = payload.stage();
        duration = Math.max(1, payload.durationTicks());
        // A cacada nao tem fim marcado: ela dura ate acabar de um jeito ou de outro.
        ticksLeft = payload.stage() == SkinwalkerPayload.Stage.HUNT ? Integer.MAX_VALUE : duration;
        if (stage == SkinwalkerPayload.Stage.REVEAL) {
            // O mundo toca o estalo de osso de onde ele esta; isto aqui e o que a pessoa ouve por
            // dentro, garantido, mesmo de costas e longe.
            BackroomsAudio.scare(NexusSounds.SKINWALKER_REVEAL.get(), 0.9F, 1.0F);
        } else if (stage == SkinwalkerPayload.Stage.HUNT) {
            BackroomsAudio.startChase();
        } else if (stage == SkinwalkerPayload.Stage.CAUGHT) {
            BackroomsAudio.stopChase();
            BackroomsAudio.scare(NexusSounds.SKINWALKER_JUMPSCARE.get(), 1.0F, 1.0F);
            // O chiado da tela tambem se ouve: o sinal caiu.
            BackroomsAudio.scare(NexusSounds.GLITCH.get(), 0.8F, 0.9F);
        }
    }

    public static void clear() {
        stage = null;
        ticksLeft = 0;
        BackroomsAudio.stopChase();
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (stage == null || event.getEntity() != Minecraft.getInstance().player) {
            return;
        }
        // Rede de seguranca: saiu das backrooms, acabou. Sem isto, sair pela porta no meio da
        // perseguicao deixava a camera tremendo no mundo normal.
        if (!Minecraft.getInstance().player.level().dimension().equals(BackroomsDimensions.BACKROOMS)) {
            clear();
            return;
        }
        if (ticksLeft != Integer.MAX_VALUE && --ticksLeft <= 0) {
            // Depois da revelacao vem a cacada; depois de ser pego, a tela volta ao normal sozinha.
            stage = stage == SkinwalkerPayload.Stage.REVEAL ? SkinwalkerPayload.Stage.HUNT : null;
            ticksLeft = stage == null ? 0 : Integer.MAX_VALUE;
            if (stage == null) {
                BackroomsAudio.stopChase();
            }
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** Quanto a camera deve tremer agora. Zero fora do susto. */
    public static double shake() {
        if (stage == null) {
            return 0.0;
        }
        return switch (stage) {
            case REVEAL -> MAX_SHAKE * progress();
            case HUNT -> MAX_SHAKE * 0.35;
            // So o golpe sacode; depois disso a tela ja esta preta e nao ha o que tremer.
            case CAUGHT -> duration - ticksLeft < CAUGHT_IMPACT_TICKS ? MAX_SHAKE * 2.0 : 0.0;
            case CLEAR -> 0.0;
        };
    }

    private static float progress() {
        return stage == SkinwalkerPayload.Stage.REVEAL
                ? Mth.clamp((duration - ticksLeft) / (float) duration, 0.0F, 1.0F)
                : 1.0F;
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (stage == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        switch (stage) {
            case REVEAL -> {
                float progress = progress();
                // O mundo vai fechando em volta enquanto ele estala os ossos.
                vignette(graphics, width, height, 0.15F + 0.85F * progress, 0.05F, 0.02F, 0.6F * progress);
                graphics.fill(0, 0, width, height, (int) (90 * progress) << 24);
            }
            case HUNT -> {
                // Pulso vermelho: da o ritmo de corrida sem tapar a visao.
                float pulse = 0.55F + 0.45F * Mth.sin(Minecraft.getInstance().player.tickCount * 0.35F);
                vignette(graphics, width, height, 1.0F, 0.06F, 0.04F, 0.35F + 0.25F * pulse);
            }
            case CAUGHT -> caught(graphics, width, height,
                    duration - ticksLeft + event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }

    /**
     * Pegou: a tela de morte das backrooms.
     *
     * <p>Corte seco, sem escurecer aos poucos — escurecer devagar deixava o corredor (e a propria
     * cabeca no chao) a vista, e o susto virava espera. No mesmo quadro: preto total, um clarao
     * vermelho do golpe e o chiado de uma TV que perdeu o sinal, com uma faixa mais forte descendo e
     * linhas rasgadas. Aos poucos o chiado assenta e aparece "Voce foi pego." tremendo, com as cores
     * se descolando como imagem com defeito. No fim tudo se apaga no preto, e a volta ao mundo abre
     * desse mesmo preto.
     */
    private static void caught(GuiGraphics graphics, int width, int height, float elapsed) {
        graphics.fill(0, 0, width, height, 0xFF000000);

        if (elapsed < CAUGHT_IMPACT_TICKS) {
            int flash = (int) (230 * (1.0F - elapsed / CAUGHT_IMPACT_TICKS));
            graphics.fill(0, 0, width, height, (flash << 24) | 0x00A00000);
        }

        float fade = 1.0F - Mth.clamp((elapsed - (CAUGHT_RETURN_AT - CAUGHT_FADE_TICKS)) / CAUGHT_FADE_TICKS, 0.0F, 1.0F);
        long frame = ScreenNoise.frame(Util.getMillis());
        RandomSource random = ScreenNoise.random(frame);

        // Forte no golpe, assentando ate um chiado de fundo — que de vez em quando ainda da um tranco.
        float settle = Mth.clamp((elapsed - CAUGHT_IMPACT_TICKS) / (CAUGHT_TITLE_AT - CAUGHT_IMPACT_TICKS), 0.0F, 1.0F);
        float noise = Mth.lerp(settle, 1.0F, 0.4F);
        if (random.nextFloat() < 0.08F) {
            noise = Math.max(noise, 0.85F);
        }
        ScreenNoise.draw(graphics, width, height, noise * fade, frame, random);

        vignette(graphics, width, height, 0.55F, 0.0F, 0.0F, 0.5F * fade);

        float title = Mth.clamp((elapsed - CAUGHT_TITLE_AT) / 8.0F, 0.0F, 1.0F) * fade;
        if (title > 0.02F) {
            caughtTitle(graphics, width, height, title, random);
        }
    }

    /** "Voce foi pego." grande, no meio, tremendo, com o vermelho e o ciano se descolando. */
    private static void caughtTitle(GuiGraphics graphics, int width, int height, float alpha, RandomSource random) {
        Font font = Minecraft.getInstance().font;
        Component title = Component.translatable("nexuscore.backrooms.caught_title");
        int shake = random.nextFloat() < 0.3F ? random.nextInt(3) - 1 : 0;
        int split = 1 + (random.nextFloat() < 0.15F ? random.nextInt(3) : 0);
        int x = -font.width(title) / 2 + shake;
        int y = -font.lineHeight / 2;
        int main = Math.max(4, (int) (alpha * 255));
        int ghost = Math.max(4, (int) (alpha * 110));

        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, height / 2.0F - 8.0F, 0.0F);
        graphics.pose().scale(3.0F, 3.0F, 1.0F);
        graphics.drawString(font, title, x - split, y, (ghost << 24) | 0xFF2020, false);
        graphics.drawString(font, title, x + split, y, (ghost << 24) | 0x20FFFF, false);
        graphics.drawString(font, title, x, y, (main << 24) | 0xC81414, false);
        graphics.pose().popPose();
    }

    private static void vignette(GuiGraphics graphics, int width, int height,
                                 float red, float green, float blue, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(red, green, blue, Mth.clamp(alpha, 0.0F, 1.0F));
        graphics.blit(VIGNETTE, 0, 0, 0.0F, 0.0F, width, height, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private SkinwalkerEffects() {}
}
