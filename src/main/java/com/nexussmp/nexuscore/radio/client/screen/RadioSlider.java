package com.nexussmp.nexuscore.radio.client.screen;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

/**
 * Uma barra de volume igual as do menu de opcoes: o mesmo widget, o mesmo desenho, o mesmo
 * "Nome: 70%".
 *
 * <p>Quem so mexe no proprio volume aplica na hora, enquanto arrasta — e o que deixa acertar de
 * ouvido. Quem mexe no volume que vale para o servidor inteiro so manda quando <b>solta</b> o
 * mouse: arrastar mandaria um pacote por pixel.
 *
 * <p>Por isso a barra so escuta o "soltar" quando foi ela que recebeu o "apertar". Sem essa
 * trava, trocar de tela em cima de uma barra dava dois cliques: o botao tocava o clique dele,
 * a tela trocava no meio do caminho, e o soltar do mouse caia na barra da tela nova — que tocava
 * o proprio clique e ainda mandava um volume que ninguem pediu.
 */
public final class RadioSlider extends AbstractSliderButton {
    private final String key;
    /** O nome que entra antes do numero (a voz da pessoa, por exemplo), ou nada. */
    @Nullable
    private final String name;
    private final IntConsumer onChange;
    private final boolean live;
    /** Se o clique que esta em andamento comecou nesta barra. */
    private boolean pressed;

    public RadioSlider(int x, int y, int width, int height, String key, @Nullable String name,
                       int percent, boolean live, IntConsumer onChange) {
        super(x, y, width, height, Component.empty(), Mth.clamp(percent, 0, 100) / 100.0);
        this.key = key;
        this.name = name;
        this.onChange = onChange;
        this.live = live;
        updateMessage();
    }

    public int percent() {
        return (int) Math.round(value * 100.0);
    }

    @Override
    protected void updateMessage() {
        setMessage(name == null
                ? Component.translatable(key, percent())
                : Component.translatable(key, name, percent()));
    }

    @Override
    protected void applyValue() {
        updateMessage();
        if (live) {
            onChange.accept(percent());
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        pressed = true;
        super.onClick(mouseX, mouseY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (!pressed) {
            // Soltar sem ter apertado aqui: e o resto de um clique dado na tela anterior.
            return;
        }
        pressed = false;
        super.onRelease(mouseX, mouseY);
        if (!live) {
            onChange.accept(percent());
        }
    }
}
