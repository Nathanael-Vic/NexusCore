package com.nexussmp.nexuscore.radio.client;

import com.nexussmp.nexuscore.radio.client.screen.RadioScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * O caminho para entrar e sair da transmissao: um botao na tela de pausa.
 *
 * <p>Durante o jogo o mouse pertence a camera — nao ha onde clicar, e obrigar uma tecla nova so
 * para isso seria pedir que cada pessoa configurasse o proprio teclado antes de ouvir uma radio.
 * A pausa e o lugar onde o Minecraft ja coloca tudo o que se faz com o mouse.
 *
 * <p>Fica no canto de baixo a esquerda, fora da grade de botoes do jogo: assim nada se desloca, o
 * menu continua com a cara de sempre e outros mods que mexem na mesma tela nao brigam por espaco.
 * Sem transmissao no ar ele fica apagado, do jeito que o jogo apaga qualquer botao sem funcao — e
 * acende sozinho no instante em que alguem entra no ar.
 */
public final class RadioPauseButton {
    private static final int WIDTH = 116;
    private static final int HEIGHT = 20;
    private static final int MARGIN = 8;

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof PauseScreen pause) || !pause.showsPauseMenu()) {
            return;
        }
        boolean live = RadioClient.live() != null;
        // Com alguem no ar, o ponto vermelho; sem ninguem, e so a porta do locutor para abrir o seu.
        Component name = Component.translatable("nexuscore.radio.pause_button").withStyle(ChatFormatting.WHITE);
        Component label = live
                ? Component.literal("● ").withStyle(ChatFormatting.RED).append(name)
                : name;
        Button button = Button.builder(label, ignored -> open(screen))
                .bounds(MARGIN, screen.height - HEIGHT - MARGIN, WIDTH, HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(live
                        ? "nexuscore.radio.pause_button.tip"
                        : "nexuscore.radio.pause_button.off")))
                .build();
        // Sem transmissao, o botao fica apagado em vez de sumir: da para ver que a radio existe e
        // que ela vai acender quando alguem entrar no ar. Quem e locutor clica mesmo assim, porque
        // e por ali que ele abre o proprio programa.
        button.active = live || RadioClient.mayBroadcast();
        event.addListener(button);
    }

    private static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new RadioScreen(parent));
    }

    private RadioPauseButton() {}
}
