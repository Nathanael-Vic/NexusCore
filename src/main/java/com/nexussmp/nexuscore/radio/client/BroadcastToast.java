package com.nexussmp.nexuscore.radio.client;

import com.nexussmp.nexuscore.core.client.PlayerSkins;
import com.nexussmp.nexuscore.radio.network.RadioPayloads;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * O aviso de que alguem entrou no ar, com a mesma cara de uma conquista.
 *
 * <p>Usa a moldura de conquista do proprio jogo e o mesmo tempo de tela: aparece no canto, escreve
 * duas linhas e sai sozinho. No lugar do icone do item vai a <b>cara de quem esta apresentando</b> —
 * e o jeito mais direto de dizer quem esta falando sem gastar uma linha de texto com isso.
 *
 * <p>Aviso do jogo nao tem botao: durante a partida o mouse esta preso na camera, e nao ha o que
 * clicar. Entao o caminho para entrar e o botao no chat (que o servidor manda junto) e o da tela de
 * pausa — os dois lugares onde o Minecraft realmente deixa clicar em alguma coisa.
 */
public final class BroadcastToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
    /** O mesmo tempo da conquista: some sozinho em cinco segundos. */
    private static final long DISPLAY_TIME = 5000L;
    /** Espaco de texto que sobra ao lado da cara. */
    private static final int TEXT_WIDTH = 125;
    private static final int TEXT_X = 30;
    private static final int LIVE_COLOR = 0xFFFF5555;

    private final RadioPayloads.Live live;

    BroadcastToast(RadioPayloads.Live live) {
        this.live = live;
    }

    /** Uma por transmissao: o mesmo aviso nao entra duas vezes na fila. */
    @Override
    public Object getToken() {
        return live.id();
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent component, long timeVisible) {
        graphics.blitSprite(BACKGROUND, 0, 0, width(), height());
        Font font = component.getMinecraft().font;

        PlayerFaceRenderer.draw(graphics,
                PlayerSkins.of(live.presenterId(), live.presenterName(), live.presenterId()), 8, 8, 16);
        graphics.drawString(font, fit(font, Component.translatable("nexuscore.radio.toast.title",
                live.presenterName())), TEXT_X, 7, LIVE_COLOR, false);
        graphics.drawString(font, fit(font, Component.literal(live.title())), TEXT_X, 18, -1, false);

        return timeVisible >= DISPLAY_TIME * component.getNotificationDisplayTimeMultiplier()
                ? Visibility.HIDE
                : Visibility.SHOW;
    }

    /** Uma linha so: o que nao couber fica de fora, como no aviso de conquista. */
    private static FormattedCharSequence fit(Font font, Component text) {
        List<FormattedCharSequence> lines = font.split(text, TEXT_WIDTH);
        return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
    }
}
