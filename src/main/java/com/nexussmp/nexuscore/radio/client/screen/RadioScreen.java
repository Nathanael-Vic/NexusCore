package com.nexussmp.nexuscore.radio.client.screen;

import com.nexussmp.nexuscore.core.client.PlayerSkins;
import com.nexussmp.nexuscore.radio.Membership;
import com.nexussmp.nexuscore.radio.client.RadioClient;
import com.nexussmp.nexuscore.radio.network.RadioPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A tela da radio: quem esta no ar, sobre o que, ha quanto tempo — e o botao de entrar ou sair.
 *
 * <p>O tamanho de tudo e <b>calculado uma vez</b>, no {@code init}, e guardado: o desenho e os
 * botoes leem a mesma conta. Foi o que consertou o texto por cima do botao — antes cada parte
 * escolhia a propria altura, e em algumas escalas de interface as duas escolhiam o mesmo lugar.
 * Quando a tela e baixa demais para tudo, o medidor de som sai primeiro; o que importa sempre cabe.
 *
 * <p>O volume aqui e <b>o seu</b>: mexer nele nao encosta no volume do jogo nem no do chat de voz.
 * O que vale para todo mundo — a musica e a voz de cada um — fica na mesa de som, que so quem
 * apresenta abre.
 */
public final class RadioScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 240;
    private static final int PADDING = 8;
    private static final int LINE = 11;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int MAX_TITLE_LINES = 2;

    @Nullable
    private final Screen parent;

    /** A conta da tela, refeita a cada {@code init}. */
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int textX;
    private int cardHeight;
    private boolean showWave;
    private List<FormattedCharSequence> titleLines = List.of();

    public RadioScreen(@Nullable Screen parent) {
        super(Component.translatable("nexuscore.radio.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        RadioPayloads.Live live = RadioClient.live();
        panelWidth = Math.min(MAX_PANEL_WIDTH, width - 2 * PADDING);
        panelX = (width - panelWidth) / 2;
        textX = panelX + PADDING + (live == null ? 0 : RadioPanel.SLOT_SIZE + PADDING + 4);
        int textWidth = panelX + panelWidth - PADDING - textX;
        titleLines = live == null
                ? List.of()
                : font.split(Component.literal(live.title()), Math.max(40, textWidth)).stream()
                        .limit(MAX_TITLE_LINES)
                        .toList();
        cardHeight = live == null
                ? 0
                : Math.max(RadioPanel.SLOT_SIZE, (titleLines.size() + (live.music().playing() ? 3 : 2)) * LINE);

        int buttonsHeight = buttonCount(live) * (BUTTON_HEIGHT + BUTTON_GAP) - BUTTON_GAP;
        // Primeiro a conta com tudo; se nao couber na altura da tela, o medidor de som sai.
        showWave = live != null;
        panelHeight = panelHeight(live);
        if (panelHeight + PADDING + buttonsHeight > height - 2 * PADDING && showWave) {
            showWave = false;
            panelHeight = panelHeight(live);
        }
        panelY = Math.max(PADDING, (height - (panelHeight + PADDING + buttonsHeight)) / 2);

        // ---- o volume de quem esta olhando, dentro do painel ----
        if (live != null) {
            addRenderableWidget(new RadioSlider(panelX + PADDING, volumeY(), panelWidth - 2 * PADDING,
                    BUTTON_HEIGHT, "nexuscore.radio.screen.volume", null, RadioClient.personalVolume(),
                    true, RadioClient::setPersonalVolume));
        }

        int buttonY = panelY + panelHeight + PADDING;
        int buttonWidth = Math.min(200, panelWidth);
        int buttonX = (width - buttonWidth) / 2;

        if (live != null) {
            boolean listening = RadioClient.listening();
            addRenderableWidget(Button.builder(
                            Component.translatable(listening
                                    ? "nexuscore.radio.screen.leave"
                                    : "nexuscore.radio.screen.listen"),
                            button -> {
                                if (listening) {
                                    RadioClient.leave();
                                } else {
                                    RadioClient.listen();
                                }
                                // Nada muda agora: quem troca o botao e a resposta do servidor,
                                // que chama refresh() ao chegar.
                            })
                    .bounds(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT).build());
            buttonY += BUTTON_HEIGHT + BUTTON_GAP;

            if (RadioClient.mayMix()) {
                addRenderableWidget(Button.builder(Component.translatable("nexuscore.radio.screen.mixer"),
                                button -> {
                                    if (minecraft != null) {
                                        // Quem abre a mesa e esta tela, entao e para ca que o
                                        // "voltar" dela tem de trazer a pessoa de volta.
                                        minecraft.setScreen(new RadioMixerScreen(this));
                                    }
                                })
                        .bounds(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT).build());
                buttonY += BUTTON_HEIGHT + BUTTON_GAP;
            }

            if (RadioClient.mayEnd()) {
                addRenderableWidget(Button.builder(Component.translatable("nexuscore.radio.screen.end"),
                                button -> {
                                    RadioClient.end();
                                    onClose();
                                })
                        .bounds(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT).build());
                buttonY += BUTTON_HEIGHT + BUTTON_GAP;
            }
        } else if (RadioClient.mayBroadcast()) {
            addRenderableWidget(Button.builder(Component.translatable("nexuscore.radio.screen.start"),
                            button -> {
                                if (minecraft != null) {
                                    minecraft.setScreen(new BroadcastStartScreen(this));
                                }
                            })
                    .bounds(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT).build());
            buttonY += BUTTON_HEIGHT + BUTTON_GAP;
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT).build());
    }

    private int buttonCount(@Nullable RadioPayloads.Live live) {
        int count = 1; // voltar
        if (live != null) {
            count++; // ouvir ou sair
            if (RadioClient.mayMix()) {
                count++; // mesa de som
            }
            if (RadioClient.mayEnd()) {
                count++; // encerrar
            }
        } else if (RadioClient.mayBroadcast()) {
            count++; // abrir o ar
        }
        return count;
    }

    /** O alto do conteudo do painel, logo abaixo da faixa. */
    private int contentY() {
        return panelY + PADDING / 2 + RadioPanel.HEADER_HEIGHT + PADDING;
    }

    private int waveY() {
        return contentY() + cardHeight + PADDING;
    }

    private int volumeY() {
        return waveY() + (showWave ? RadioPanel.WAVE_HEIGHT + PADDING : 0);
    }

    private int panelHeight(@Nullable RadioPayloads.Live live) {
        if (live == null) {
            // Ar vazio: so a faixa e uma linha dizendo isso.
            return PADDING + RadioPanel.HEADER_HEIGHT + PADDING + LINE + PADDING;
        }
        int wave = showWave ? RadioPanel.WAVE_HEIGHT + PADDING : 0;
        return PADDING + RadioPanel.HEADER_HEIGHT + PADDING + cardHeight + PADDING + wave
                + BUTTON_HEIGHT + PADDING;
    }

    /**
     * Refaz os botoes quando chega estado novo do servidor.
     *
     * <p>O clique so pede; quem manda e a resposta. E por isso que o botao vira "sair" apenas
     * depois que o servidor confirmou que a pessoa entrou mesmo.
     */
    public void refresh() {
        rebuildWidgets();
    }

    /**
     * O painel e <b>fundo</b>, e por isso e pintado aqui.
     *
     * <p>Desenhar depois dos botoes parecia funcionar e nao funcionava: o jogo junta os retangulos
     * num lote e o texto em outro, entao o painel cobria a moldura dos botoes e deixava as letras
     * por cima — botao virava texto solto. Fundo no lugar de fundo, widget por cima: cada um no seu.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        RadioPayloads.Live live = RadioClient.live();

        RadioPanel.panel(graphics, panelX, panelY, panelWidth, panelHeight);
        RadioPanel.header(graphics, font, panelX + PADDING / 2, panelY + PADDING / 2, panelWidth - PADDING,
                Component.translatable(live == null ? "nexuscore.radio.hud.off" : "nexuscore.radio.hud.live"),
                live == null ? null : headerRight());

        int contentY = contentY();
        if (live == null) {
            graphics.drawCenteredString(font, Component.translatable("nexuscore.radio.screen.off"),
                    width / 2, contentY, RadioPanel.MUTED_TEXT);
            return;
        }

        RadioPanel.slot(graphics, panelX + PADDING, contentY);
        PlayerFaceRenderer.draw(graphics,
                PlayerSkins.of(live.presenterId(), live.presenterName(), live.presenterId()),
                panelX + PADDING + 1, contentY + 1, RadioPanel.SLOT_SIZE - 2);

        int line = contentY;
        for (FormattedCharSequence titleLine : titleLines) {
            graphics.drawString(font, titleLine, textX, line, RadioPanel.TEXT, false);
            line += LINE;
        }
        graphics.drawString(font, Component.translatable("nexuscore.radio.screen.presenter",
                live.presenterName()), textX, line, RadioPanel.MUTED_TEXT, false);
        line += LINE;
        graphics.drawString(font, status(), textX, line, 0xFFAA00, false);
        if (live.music().playing()) {
            line += LINE;
            graphics.drawString(font, Component.translatable(live.music().paused()
                            ? "nexuscore.radio.screen.music_paused"
                            : "nexuscore.radio.screen.music_playing"),
                    textX, line, RadioPanel.MUTED_TEXT, false);
        }

        if (showWave) {
            RadioPanel.waveform(graphics, panelX + PADDING, waveY(), panelWidth - 2 * PADDING,
                    RadioClient.listening());
        }
    }

    /**
     * O canto direito do letreiro: o tempo no ar e, para quem esta com o microfone fechado, o aviso
     * de que esta mudo — a informacao que mais importa saber de relance.
     */
    private static Component headerRight() {
        Component elapsed = Component.translatable("nexuscore.radio.screen.elapsed_short",
                Mth.clamp(RadioClient.elapsedSeconds() / 60, 0, 999));
        return RadioClient.micMuted()
                ? Component.translatable("nexuscore.radio.mic.tag").withStyle(ChatFormatting.RED)
                        .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(elapsed)
                : elapsed;
    }

    /** Ouvindo, convidado, no ar: a palavra que descreve onde a pessoa esta nesta transmissao. */
    private static Component status() {
        Membership.Role role = RadioClient.membership().role();
        return Component.translatable(switch (role) {
            case ON_AIR -> "nexuscore.radio.status.on_air";
            case INVITED -> "nexuscore.radio.status.invited";
            case LISTENING -> "nexuscore.radio.status.listening";
            case NOT_LISTENING -> "nexuscore.radio.status.not_listening";
        });
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    /** Vem da pausa: o jogo continua parado por tras, como em qualquer tela do menu. */
    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
