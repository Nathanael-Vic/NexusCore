package com.nexussmp.nexuscore.relic.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.nexussmp.nexuscore.core.client.ScreenNoise;
import com.nexussmp.nexuscore.relic.astral.AstralPayloads;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A cabeca de quem esta fora do corpo, cedendo.
 *
 * <p>Tudo cresce com o tempo la fora — a "loucura", de 0 (acabou de sair) a 1 (o limite) e alem:
 * <ul>
 *   <li><b>Logo:</b> um veu roxo e as bordas escurecendo. Ainda da para ignorar.</li>
 *   <li><b>Um terco:</b> sussurros (sons de caverna vindo de lugar nenhum), a camera comeca a
 *       pender, a visao embaca de vez em quando.</li>
 *   <li><b>Metade:</b> o coracao bate alto, frases aparecem e somem pela tela, chiado de TV, a
 *       imagem rasga em faixas, a visao se parte em varias (o olhar da aranha).</li>
 *   <li><b>Perto do fim:</b> cores invertendo, tudo tremendo e girando, o coracao disparado, um
 *       grito ao fundo, e um "VOLTE" no meio da tela.</li>
 *   <li><b>Passou do limite:</b> a maldicao do guardiao, a tela vermelha, tudo alternando sem
 *       parar — e o dano, do lado do servidor.</li>
 * </ul>
 * Os filtros da tela (desfoque, aranha, creeper, inverter) sao os do proprio jogo, os mesmos de
 * espectar uma criatura.
 */
public final class AstralClient {
    public static final KeyMapping RETURN_KEY = new KeyMapping("key.nexuscore.astral_return",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.nexuscore");

    private static final ResourceLocation BLUR = ResourceLocation.withDefaultNamespace("shaders/post/blur.json");
    private static final ResourceLocation SPIDER = ResourceLocation.withDefaultNamespace("shaders/post/spider.json");
    private static final ResourceLocation CREEPER = ResourceLocation.withDefaultNamespace("shaders/post/creeper.json");
    private static final ResourceLocation INVERT = ResourceLocation.withDefaultNamespace("shaders/post/invert.json");
    private static final ResourceLocation[] CHAOS = {BLUR, SPIDER, CREEPER, INVERT};
    private static final ResourceLocation VIGNETTE = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");

    /** Quantas frases existem no arquivo de idioma ({@code ...astral_mask.whisper.0} a N-1). */
    private static final int WHISPER_COUNT = 14;
    private static final int MAX_WHISPERS = 12;
    /** Quanto tempo o aviso de como voltar fica na tela. */
    private static final int HINT_TICKS = 100;
    /** Ate onde a alma enxerga ao sair do corpo, e ate onde no limite, em blocos. */
    private static final float SIGHT_FAR = 22.0F;
    private static final float SIGHT_NEAR = 12.0F;

    private static boolean active;
    private static int ticks;
    private static int maxTicks = 1;
    @Nullable
    private static ResourceLocation post;
    private static int postHold;
    private static boolean shrieked;
    private static boolean cursed;
    private static final List<Whisper> WHISPERS = new ArrayList<>();

    /** Uma frase solta na tela: onde (fracao da tela), qual, o tamanho e quando nasceu. */
    private record Whisper(int key, float x, float y, float scale, int born, int life) {}

    public static void apply(AstralPayloads.State state) {
        if (state.active()) {
            stop();
            active = true;
            maxTicks = Math.max(1, state.maxTicks());
        } else {
            stop();
        }
    }

    public static void stop() {
        active = false;
        ticks = 0;
        shrieked = false;
        cursed = false;
        WHISPERS.clear();
        if (post != null) {
            post = null;
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gameRenderer.shutdownEffect();
            minecraft.gameRenderer.checkEntityPostEffect(minecraft.getCameraEntity());
        }
    }

    private static float madness(float partialTick) {
        return (ticks + partialTick) / maxTicks;
    }

    // ---- tick: tecla, filtros, sons, frases ----

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (RETURN_KEY.consumeClick()) {
            if (active) {
                PacketDistributor.sendToServer(new AstralPayloads.Return());
            }
        }
        if (!active || minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        ticks++;
        float madness = madness(0.0F);
        RandomSource random = minecraft.level.getRandom();
        filters(minecraft.gameRenderer, madness, random);
        sounds(minecraft.player, madness, random);
        if (madness > 0.3F && WHISPERS.size() < MAX_WHISPERS && random.nextFloat() < (madness - 0.3F) * 0.15F) {
            WHISPERS.add(new Whisper(random.nextInt(WHISPER_COUNT), 0.08F + random.nextFloat() * 0.84F,
                    0.1F + random.nextFloat() * 0.8F, 1.0F + random.nextFloat() * 1.5F * Math.min(madness, 1.2F),
                    ticks, 15 + random.nextInt(25)));
        }
        WHISPERS.removeIf(whisper -> ticks - whisper.born() > whisper.life());
    }

    /** Escolhe o proximo "episodio" de filtro quando o atual acaba. Quanto mais louco, mais curtos e piores. */
    private static void filters(GameRenderer renderer, float madness, RandomSource random) {
        if (--postHold <= 0) {
            ResourceLocation next = null;
            float roll = random.nextFloat();
            if (madness < 0.3F) {
                postHold = 20;
            } else if (madness < 0.55F) {
                next = roll < 0.35F ? BLUR : null;
                postHold = next != null ? 30 + random.nextInt(30) : 40 + random.nextInt(40);
            } else if (madness < 0.8F) {
                next = roll < 0.3F ? SPIDER : roll < 0.7F ? BLUR : null;
                postHold = next == SPIDER ? 10 + random.nextInt(15) : 25 + random.nextInt(25);
            } else if (madness < 1.0F) {
                next = roll < 0.3F ? INVERT : roll < 0.6F ? SPIDER : roll < 0.9F ? BLUR : CREEPER;
                postHold = next == INVERT ? 3 + random.nextInt(6) : 10 + random.nextInt(15);
            } else {
                next = CHAOS[random.nextInt(CHAOS.length)];
                postHold = 3 + random.nextInt(6);
            }
            if (!Objects.equals(next, post)) {
                post = next;
                if (next == null) {
                    renderer.shutdownEffect();
                } else {
                    renderer.loadEffect(next);
                }
            }
        }
        PostChain chain = renderer.currentEffect();
        if (post == BLUR && chain != null) {
            chain.setUniform("Radius", 3.0F + 7.0F * Math.min(madness, 1.2F));
        }
    }

    private static void sounds(LocalPlayer player, float madness, RandomSource random) {
        // Sussurros: um som de caverna vindo de um lugar qualquer em volta.
        if (random.nextFloat() < 0.002F + 0.03F * madness * madness) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 3.0 + random.nextDouble() * 5.0;
            local(player, player.getX() + Math.cos(angle) * distance, player.getEyeY() + random.nextGaussian(),
                    player.getZ() + Math.sin(angle) * distance, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT,
                    0.5F + 0.5F * Math.min(madness, 1.0F), 0.6F + random.nextFloat() * 0.6F);
        }
        // O coracao: comeca na metade, cada vez mais depressa.
        if (madness > 0.45F) {
            int interval = (int) Mth.lerp(Mth.clamp((madness - 0.45F) / 0.55F, 0.0F, 1.0F), 40.0F, 9.0F);
            if (ticks % interval == 0) {
                local(player, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.PLAYERS, 0.6F + 0.6F * Math.min(madness, 1.0F), 1.0F);
            }
        }
        if (madness > 0.6F && random.nextFloat() < 0.004F) {
            local(player, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ENDERMAN_STARE, SoundSource.AMBIENT, 0.5F, 0.6F);
        }
        if (madness >= 0.75F && !shrieked) {
            shrieked = true;
            local(player, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.AMBIENT, 1.0F, 0.8F);
        }
        if (madness >= 1.0F && !cursed) {
            cursed = true;
            local(player, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.AMBIENT, 1.0F, 0.9F);
        }
    }

    private static void local(LocalPlayer player, double x, double y, double z, SoundEvent sound, SoundSource source,
                              float volume, float pitch) {
        player.level().playLocalSound(x, y, z, sound, source, volume, pitch, false);
    }

    // ---- camera ----

    /** A camera pende de um lado para o outro, e no fim treme. */
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active) {
            return;
        }
        float time = ticks + (float) event.getPartialTick();
        float madness = madness((float) event.getPartialTick());
        float roll = Mth.sin(time * 0.035F) * 22.0F * madness * madness;
        if (madness > 0.7F) {
            float shake = (madness - 0.7F) * 12.0F;
            roll += Mth.sin(time * 1.7F) * Mth.sin(time * 0.9F) * shake;
            event.setYaw(event.getYaw() + Mth.sin(time * 2.3F) * shake * 0.25F);
            event.setPitch(event.getPitch() + Mth.cos(time * 1.9F) * shake * 0.25F);
        }
        event.setRoll(event.getRoll() + roll);
    }

    /**
     * A alma nao enxerga longe: uma nevoa roxa fecha a visao em {@value #SIGHT_FAR} blocos, e aperta
     * ate {@value #SIGHT_NEAR} conforme a loucura cresce. E o que impede de atravessar o chao e varrer
     * o mapa atras de construcao: da para ver o que esta perto, nao o que esta longe. Vale para tudo —
     * terreno, agua, ceu e criaturas —, porque e a propria nevoa do jogo.
     */
    static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!active) {
            return;
        }
        float madness = Mth.clamp(madness((float) event.getPartialTick()), 0.0F, 1.0F);
        float far = Mth.lerp(madness, SIGHT_FAR, SIGHT_NEAR);
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * 0.2F));
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), far));
        event.setFogShape(FogShape.SPHERE);
        // Sem cancelar, o jogo ignora as distancias novas.
        event.setCanceled(true);
    }

    /** A nevoa e roxa-escura, quase preta: a cor do "outro plano". */
    static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (active) {
            event.setRed(0.07F);
            event.setGreen(0.02F);
            event.setBlue(0.11F);
        }
    }

    /** O campo de visao respira — e no fim, puxa e solta. */
    static void onFov(ViewportEvent.ComputeFov event) {
        if (!active) {
            return;
        }
        float time = ticks + (float) event.getPartialTick();
        float madness = madness((float) event.getPartialTick());
        double scale = 1.0 + 0.1 * madness * Mth.sin(time * 0.06F);
        if (madness > 0.8F) {
            scale += 0.15 * Mth.sin(time * 0.5F) * Math.min(1.0F, (madness - 0.8F) * 5.0F);
        }
        event.setFOV(event.getFOV() * scale);
    }

    // ---- a tela ----

    static void onRenderGui(RenderGuiEvent.Post event) {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = ticks + partial;
        float madness = madness(partial);
        long frame = ScreenNoise.frame(Util.getMillis());
        RandomSource random = ScreenNoise.random(frame);

        // O veu: roxo enquanto da, vermelho pulsando depois do limite.
        if (madness < 1.0F) {
            int alpha = (int) (Mth.clamp(0.08F + 0.25F * madness, 0.0F, 0.45F) * 255);
            graphics.fill(0, 0, width, height, (alpha << 24) | 0x2A0A3A);
        } else {
            int alpha = (int) ((0.3F + 0.15F * Mth.sin(time * 0.4F)) * 255);
            graphics.fill(0, 0, width, height, (alpha << 24) | 0x5A0000);
        }
        vignette(graphics, width, height, madness < 1.0F ? 0.2F : 0.6F, 0.0F, madness < 1.0F ? 0.25F : 0.0F,
                Math.min(1.0F, 0.3F + 0.7F * madness));

        float noise = madness < 0.4F ? 0.0F : (madness - 0.4F) * 0.5F;
        if (random.nextFloat() < 0.05F * madness) {
            noise = Math.max(noise, 0.8F * Math.min(madness, 1.0F));
        }
        ScreenNoise.draw(graphics, width, height, Math.min(noise, 0.7F), frame, random);

        if (madness > 0.5F) {
            tears(graphics, width, height, madness, random);
        }
        whispers(graphics, minecraft.font, width, height, time, random);
        if (madness > 0.85F) {
            comeBack(graphics, minecraft.font, width, height, time, madness, random);
        }
        if (ticks < HINT_TICKS) {
            Component hint = Component.translatable("nexuscore.relic.astral_mask.hint", RETURN_KEY.getTranslatedKeyMessage());
            int alpha = (int) (Mth.clamp((HINT_TICKS - time) / 20.0F, 0.0F, 1.0F) * 200) + 4;
            graphics.drawCenteredString(minecraft.font, hint, width / 2, height - 60, (alpha << 24) | 0xD8C8FF);
        }
    }

    /** Faixas da imagem que escorregam para o lado, em magenta e ciano. */
    private static void tears(GuiGraphics graphics, int width, int height, float madness, RandomSource random) {
        if (random.nextFloat() > madness) {
            return;
        }
        int count = (int) (Math.min(madness, 1.3F) * 6);
        for (int i = 0; i < count; i++) {
            int y = random.nextInt(height);
            int thickness = 1 + random.nextInt(4);
            int shift = random.nextInt(31) - 15;
            int color = random.nextBoolean() ? 0xFF20E0 : 0x20F0FF;
            int alpha = 60 + random.nextInt(100);
            graphics.fill(shift, y, width + shift, y + thickness, (alpha << 24) | color);
        }
    }

    /** As frases soltas: aparecem, piscam e somem, com as cores se descolando. */
    private static void whispers(GuiGraphics graphics, Font font, int width, int height, float time, RandomSource random) {
        Iterator<Whisper> iterator = WHISPERS.iterator();
        while (iterator.hasNext()) {
            Whisper whisper = iterator.next();
            float age = (time - whisper.born()) / whisper.life();
            if (age >= 1.0F || random.nextFloat() < 0.15F) {
                continue;
            }
            int alpha = Math.max(4, (int) (Mth.sin(age * Mth.PI) * 220));
            Component text = Component.translatable("nexuscore.relic.astral_mask.whisper." + whisper.key());
            float x = whisper.x() * width;
            float y = whisper.y() * height;
            drawGlitched(graphics, font, text, x, y, whisper.scale(), alpha, 0xE8E0FF, random);
        }
    }

    /** "VOLTE" enorme no meio — e, passado o limite, o aviso de que o corpo esta morrendo. */
    private static void comeBack(GuiGraphics graphics, Font font, int width, int height, float time, float madness,
                                 RandomSource random) {
        float pulse = 0.5F + 0.5F * Mth.sin(time * 0.3F);
        int alpha = Math.max(4, (int) ((0.35F + 0.65F * pulse) * Math.min(1.0F, (madness - 0.85F) * 6.0F) * 255));
        drawGlitched(graphics, font, Component.translatable("nexuscore.relic.astral_mask.come_back"),
                width / 2.0F, height / 2.0F - 20.0F, 4.0F + pulse, alpha, 0xFF3030, random);
        if (madness >= 1.0F) {
            drawGlitched(graphics, font, Component.translatable("nexuscore.relic.astral_mask.dying"),
                    width / 2.0F, height / 2.0F + 20.0F, 1.6F, Math.max(4, (int) (pulse * 255)), 0xFFB0B0, random);
        }
    }

    /** Texto centrado em (x, y), com uma copia vermelha e uma ciano tremendo em volta. */
    private static void drawGlitched(GuiGraphics graphics, Font font, Component text, float x, float y, float scale,
                                     int alpha, int color, RandomSource random) {
        int split = 1 + (random.nextFloat() < 0.2F ? random.nextInt(3) : 0);
        int jitter = random.nextFloat() < 0.3F ? random.nextInt(3) - 1 : 0;
        int left = -font.width(text) / 2 + jitter;
        int top = -font.lineHeight / 2;
        int ghost = Math.max(4, alpha / 2);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, left - split, top, (ghost << 24) | 0xFF2020, false);
        graphics.drawString(font, text, left + split, top, (ghost << 24) | 0x20FFFF, false);
        graphics.drawString(font, text, left, top, (alpha << 24) | color, false);
        graphics.pose().popPose();
    }

    private static void vignette(GuiGraphics graphics, int width, int height, float red, float green, float blue, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(red, green, blue, Mth.clamp(alpha, 0.0F, 1.0F));
        graphics.blit(VIGNETTE, 0, 0, 0.0F, 0.0F, width, height, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private AstralClient() {}
}
