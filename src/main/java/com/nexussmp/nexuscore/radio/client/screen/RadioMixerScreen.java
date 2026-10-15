package com.nexussmp.nexuscore.radio.client.screen;

import com.nexussmp.nexuscore.radio.client.RadioClient;
import com.nexussmp.nexuscore.radio.network.RadioPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A mesa de som de quem apresenta: o microfone, a musica e o volume de cada voz.
 *
 * <p>Tudo aqui vale <b>para quem esta ouvindo</b>, e nao so para quem mexe: e a mistura do
 * programa. O volume de cada pessoa que ouve continua sendo dela, na tela de tras.
 *
 * <p>A musica nao passa pelo servidor. Ele guarda o endereco e o ponto; cada cliente toca por
 * conta, o que e o unico jeito de uma radio de servidor ter som de musica em vez de som de
 * telefone.
 */
public final class RadioMixerScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 240;
    private static final int PADDING = 8;
    private static final int LINE = 11;
    private static final int ROW = 20;
    private static final int GAP = 4;

    @Nullable
    private final Screen parent;

    private EditBox urlField;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int voiceCount;

    public RadioMixerScreen(@Nullable Screen parent) {
        super(Component.translatable("nexuscore.radio.mixer.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<RadioPayloads.Voice> voices = RadioClient.voices();
        voiceCount = voices.size();
        panelWidth = Math.min(MAX_PANEL_WIDTH, width - 2 * PADDING);
        panelX = (width - panelWidth) / 2;
        int inner = panelWidth - 2 * PADDING;

        // A conta da altura, na mesma ordem do desenho: faixa, microfone, endereco, transporte,
        // volume da musica, uma linha por voz.
        panelHeight = PADDING + RadioPanel.HEADER_HEIGHT + PADDING
                + ROW + GAP                       // microfone
                + LINE + ROW + GAP                // endereco da musica
                + ROW + GAP                       // tocar / pausar / parar
                + LINE + ROW + GAP                // volume da musica
                + (voiceCount > 0 ? LINE + voiceCount * (ROW + GAP) : 0)
                + PADDING;
        panelY = Math.max(PADDING, (height - (panelHeight + PADDING + ROW)) / 2);

        int x = panelX + PADDING;
        int y = panelY + PADDING / 2 + RadioPanel.HEADER_HEIGHT + PADDING;

        // ---- microfone ----
        boolean muted = RadioClient.micMuted();
        boolean onAir = RadioClient.membership().onAir();
        Button mic = Button.builder(Component.translatable("nexuscore.radio.screen.mic",
                                Component.translatable(muted ? "nexuscore.radio.mic.off" : "nexuscore.radio.mic.on")
                                        .withStyle(muted ? ChatFormatting.RED : ChatFormatting.GREEN)),
                        button -> RadioClient.setMicMuted(!muted))
                .bounds(x, y, inner, ROW).build();
        mic.active = onAir;
        addRenderableWidget(mic);
        y += ROW + GAP;

        // ---- endereco da musica ----
        y += LINE;
        RadioPayloads.Music music = RadioClient.music();
        urlField = new EditBox(font, x, y, inner, ROW, Component.translatable("nexuscore.radio.mixer.url"));
        urlField.setMaxLength(RadioPayloads.MAX_URL);
        urlField.setHint(Component.translatable("nexuscore.radio.mixer.url_hint"));
        if (music.playing()) {
            urlField.setValue(music.url());
        }
        addRenderableWidget(urlField);
        y += ROW + GAP;

        // ---- tocar, pausar, parar ----
        int third = (inner - 2 * GAP) / 3;
        addRenderableWidget(Button.builder(Component.translatable("nexuscore.radio.mixer.play"),
                        button -> RadioClient.playMusic(urlField.getValue()))
                .bounds(x, y, third, ROW).build());
        Button pause = Button.builder(Component.translatable(music.paused()
                                ? "nexuscore.radio.mixer.resume"
                                : "nexuscore.radio.mixer.pause"),
                        button -> RadioClient.pauseMusic(!music.paused()))
                .bounds(x + third + GAP, y, third, ROW).build();
        pause.active = music.playing();
        addRenderableWidget(pause);
        Button stop = Button.builder(Component.translatable("nexuscore.radio.mixer.stop"),
                        button -> RadioClient.stopMusic())
                .bounds(x + 2 * (third + GAP), y, inner - 2 * (third + GAP), ROW).build();
        stop.active = music.playing();
        addRenderableWidget(stop);
        y += ROW + GAP;

        // ---- volume da musica ----
        y += LINE;
        addRenderableWidget(new RadioSlider(x, y, inner, ROW, "nexuscore.radio.mixer.music_volume",
                null, music.volume(), false, RadioClient::setMusicVolume));
        y += ROW + GAP;

        // ---- uma barra por voz no ar ----
        if (voiceCount > 0) {
            y += LINE;
            for (RadioPayloads.Voice voice : voices) {
                addRenderableWidget(new RadioSlider(x, y, inner, ROW, "nexuscore.radio.mixer.voice_volume",
                        voice.name(), voice.volume(), false,
                        volume -> RadioClient.setVoiceVolume(voice.id(), volume)));
                y += ROW + GAP;
            }
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds((width - Math.min(200, panelWidth)) / 2, panelY + panelHeight + PADDING,
                        Math.min(200, panelWidth), ROW).build());
    }

    /** Estado novo do servidor: a mesa se remonta com ele (musica trocada, voz nova no ar). */
    public void refresh() {
        String typed = urlField == null ? "" : urlField.getValue();
        rebuildWidgets();
        if (urlField != null && !typed.isEmpty()) {
            urlField.setValue(typed);
        }
    }

    /** O painel e os rotulos sao fundo: vao antes dos widgets, para eles ficarem por cima. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        RadioPanel.panel(graphics, panelX, panelY, panelWidth, panelHeight);
        RadioPanel.header(graphics, font, panelX + PADDING / 2, panelY + PADDING / 2, panelWidth - PADDING,
                title, null);

        int x = panelX + PADDING;
        int y = panelY + PADDING / 2 + RadioPanel.HEADER_HEIGHT + PADDING + ROW + GAP;
        graphics.drawString(font, Component.translatable("nexuscore.radio.mixer.url"), x, y,
                RadioPanel.MUTED_TEXT, false);
        y += LINE + ROW + GAP + ROW + GAP;
        graphics.drawString(font, Component.translatable("nexuscore.radio.mixer.music"), x, y,
                RadioPanel.MUTED_TEXT, false);
        Component playing = playingTag();
        graphics.drawString(font, playing, panelX + panelWidth - PADDING - font.width(playing), y,
                RadioClient.music().playing() ? RadioPanel.LIVE : RadioPanel.MUTED_TEXT, false);
        if (voiceCount > 0) {
            y += LINE + ROW + GAP;
            graphics.drawString(font, Component.translatable("nexuscore.radio.mixer.voices"), x, y,
                    RadioPanel.MUTED_TEXT, false);
        }
    }

    /**
     * Se esta tocando, e onde: "1:23" corre sozinho, e e o sinal de que o video pegou mesmo.
     *
     * <p>Enquanto o endereco esta sendo resolvido o tempo fica em zero; quando ele anda, a musica
     * comecou de verdade na maquina de quem ouve.
     */
    private static Component playingTag() {
        RadioPayloads.Music music = RadioClient.music();
        if (!music.playing()) {
            return Component.translatable("nexuscore.radio.mixer.idle");
        }
        long seconds = RadioClient.musicPositionMillis() / 1000L;
        String clock = String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
        return Component.translatable(music.paused()
                ? "nexuscore.radio.mixer.paused_at"
                : "nexuscore.radio.mixer.playing_at", clock);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
