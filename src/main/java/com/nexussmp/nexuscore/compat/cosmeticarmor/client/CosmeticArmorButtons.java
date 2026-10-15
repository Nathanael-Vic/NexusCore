package com.nexussmp.nexuscore.compat.cosmeticarmor.client;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Os botoes do Cosmetic Armor redesenhados no estilo do Curios.
 *
 * <p>Os dois mods fazem a mesma coisa no inventario — um simbolo que abre a tela deles e um
 * botaozinho por slot que liga e desliga a visibilidade da peca — e desenham isso de dois jeitos
 * que nao combinam. O Curios usa icones com contorno e degrade, dourados quando o mouse passa,
 * e conta o que o botao faz numa dica flutuante. O Cosmetic Armor usa um quadrado preto chapado,
 * um peitoral que fica arco-iris no hover, e escreve o nome do mod por cima do inventario.
 *
 * <p>Aqui os botoes dele saem da tela e entram estes no lugar, no desenho do Curios. O clique
 * continua sendo o dele: o botao original e guardado fora da tela so para ser apertado e para
 * dizer em que estado esta. Nada do Cosmetic Armor e alterado — se um dia ele mudar os nomes
 * dessas classes, a tela volta a ser a dele, sem quebrar nada.
 *
 * <p>Os icones ficam em {@code textures/gui/sprites/cosmetic/}. Os dois do botaozinho sao copia
 * pixel a pixel dos do Curios (ambos os mods sao LGPL); o peitoral e o desenho do proprio Cosmetic
 * Armor, repintado com a paleta de cinzas do Curios e com o dourado que ele usa no hover.
 */
public final class CosmeticArmorButtons {

    /** As duas classes de botao do Cosmetic Armor, procuradas pelo nome para nao depender do jar dele. */
    private static final String TOGGLE_BUTTON = "lain.mods.cos.impl.client.gui.GuiCosArmorToggleButton";
    private static final String ARMOR_BUTTON = "lain.mods.cos.impl.client.gui.GuiCosArmorButton";

    private static final ResourceLocation TOGGLE_VISIBLE = NexusCore.id("cosmetic/toggle_visible");
    private static final ResourceLocation TOGGLE_HIDDEN = NexusCore.id("cosmetic/toggle_hidden");
    private static final ResourceLocation ARMOR = NexusCore.id("cosmetic/armor");
    private static final ResourceLocation ARMOR_HIGHLIGHTED = NexusCore.id("cosmetic/armor_highlighted");

    /** O campo {@code public int state} do botaozinho: 1 = escondido, 0 = aparecendo. */
    private static Field stateField;
    private static boolean stateLookedUp;

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        // Desligado na config: os botoes ficam como o Cosmetic Armor desenha.
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !CommonConfig.isModuleEnabled(ModuleIds.COMPAT_COSMETICARMOR)) {
            return;
        }
        // Copia antes de mexer: a lista do evento e a propria lista de widgets da tela, e trocar
        // botao durante a volta do laco daria ConcurrentModificationException.
        for (GuiEventListener listener : List.copyOf(event.getListenersList())) {
            if (!(listener instanceof Button button)) {
                continue;
            }
            String name = button.getClass().getName();
            if (TOGGLE_BUTTON.equals(name)) {
                event.removeListener(button);
                event.addListener(new ToggleButton(screen, button));
            } else if (ARMOR_BUTTON.equals(name)) {
                event.removeListener(button);
                event.addListener(new ArmorButton(screen, button));
            }
        }
    }

    /**
     * Desenha os botaozinhos de visibilidade, depois que os slots ja passaram.
     *
     * <p>Widget nenhum serve aqui. A tela desenha os widgets primeiro e os slots depois, e o realce
     * claro do slot sob o mouse e um retangulo sem teste de profundidade — ele cobre o que ja estava
     * na tela, e o botao ficava por baixo. O Curios resolve isso desenhando os dele num segundo
     * passe, e e o que se faz aqui, so que num ponto melhor que o dele: este evento e disparado
     * depois do realce, mas ainda antes do item arrastado no cursor e antes das dicas flutuantes —
     * entao o botao cobre o slot sem cobrir o que tem de ficar por cima dele.
     *
     * <p>Aqui a matriz ja esta deslocada para o canto do inventario, por isso o desconto do
     * {@code guiLeft}/{@code guiTop}. O teste de profundidade ja vem desligado da tela e assim deve
     * continuar: religar estragaria o desenho do item no cursor, logo a seguir.
     */
    public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof ToggleButton toggle) {
                toggle.renderOnTop(event.getGuiGraphics(), screen.getGuiLeft(), screen.getGuiTop());
            }
        }
    }

    /**
     * O texto da dica. Com o Curios instalado e a chave dele, de proposito: assim os dois botoes
     * dizem exatamente a mesma coisa, no mesmo idioma, hoje e quando ele ganhar traducoes novas.
     */
    private static Component toggleLabel() {
        return ModList.get().isLoaded("curios")
                ? Component.translatable("gui.curios.toggle")
                : Component.translatable("nexuscore.cosmetic.toggle_visibility");
    }

    /** Em que estado o botao original esta. Se o campo sumir, o icone fica no "aparecendo". */
    private static boolean isHidden(Button original) {
        if (!stateLookedUp) {
            stateLookedUp = true;
            try {
                stateField = original.getClass().getField("state");
            } catch (ReflectiveOperationException | RuntimeException error) {
                NexusCore.LOGGER.warn(
                        "[NexusCore] compat.cosmeticarmor: o estado do botao de visibilidade nao pode ser lido", error);
            }
        }
        if (stateField == null) {
            return false;
        }
        try {
            return stateField.getInt(original) != 0;
        } catch (ReflectiveOperationException | RuntimeException error) {
            stateField = null;
            return false;
        }
    }

    /**
     * O botaozinho de visibilidade, no canto do slot.
     *
     * <p>Ele existe em dois lugares. Na tela de cosmeticos sao quatro, um por peca, e cada um mora
     * na quina de cima de um slot; na tela normal e um so, que desliga o cosmetico inteiro no
     * cliente e fica onde a configuracao do mod mandar. Os dois casos se distinguem sozinhos: se a
     * posicao do botao original bate com a quina de um slot da tela, ele e de slot.
     *
     * <p>Sendo de slot, ele passa a usar a conta do proprio Curios — {@code slot.x + 12},
     * {@code slot.y - 1} — e fica meio para fora, montado na divisoria. Nao sendo, ele so cresce
     * de 5 para 8 pixels em volta do mesmo centro, o que o deixa no lugar de sempre.
     */
    private static final class ToggleButton extends Button {
        private static final int SIZE = 8;

        private final AbstractContainerScreen<?> screen;
        private final Button original;
        /** O slot em cuja quina ele mora, ou {@code null} quando e o botao geral da tela normal. */
        @Nullable
        private final Slot slot;
        private final int relX;
        private final int relY;

        private ToggleButton(AbstractContainerScreen<?> screen, Button original) {
            super(original.getX(), original.getY(), SIZE, SIZE, Component.empty(),
                    ignored -> original.onPress(), DEFAULT_NARRATION);
            this.screen = screen;
            this.original = original;
            this.slot = slotCorner(screen, original);
            // O icone novo e 3 pixels maior que o quadrado antigo; recuar 1 mantem o mesmo centro.
            this.relX = original.getX() - screen.getGuiLeft() - 1;
            this.relY = original.getY() - screen.getGuiTop() - 1;
            setTooltip(Tooltip.create(toggleLabel()));
            follow();
        }

        /** O slot cuja moldura comeca exatamente onde o botao esta, se houver um. */
        @Nullable
        private static Slot slotCorner(AbstractContainerScreen<?> screen, Button button) {
            int x = button.getX() - screen.getGuiLeft();
            int y = button.getY() - screen.getGuiTop();
            for (Slot candidate : screen.getMenu().slots) {
                if (candidate.x - 1 == x && candidate.y - 1 == y) {
                    return candidate;
                }
            }
            return null;
        }

        /** O inventario anda quando o livro de receitas abre; o botao vai junto, a cada quadro. */
        private void follow() {
            if (slot != null) {
                setX(screen.getGuiLeft() + slot.x + 12);
                setY(screen.getGuiTop() + slot.y - 1);
            } else {
                setX(screen.getGuiLeft() + relX);
                setY(screen.getGuiTop() + relY);
            }
        }

        /**
         * Nao desenha nada: quem desenha e {@link #onRenderForeground}, mais tarde no quadro.
         *
         * <p>O que sobra aqui ainda importa. E deste metodo que a tela sabe onde o botao esta, e e
         * a chamada dele que faz o jogo conferir o mouse por cima e mostrar a dica.
         */
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            follow();
        }

        private void renderOnTop(GuiGraphics graphics, int guiLeft, int guiTop) {
            graphics.blitSprite(isHidden(original) ? TOGGLE_HIDDEN : TOGGLE_VISIBLE,
                    getX() - guiLeft, getY() - guiTop, getWidth(), getHeight());
        }
    }

    /**
     * O peitoral que abre e fecha a tela de cosmeticos, no canto do boneco.
     *
     * <p>Duas coisas mudam. O hover deixa de ser arco-iris e passa a ser dourado, como o do Curios,
     * e o nome que ele escrevia por cima do inventario sai — "CosmeticArmor" na tela normal,
     * "Normal" na tela de cosmeticos. O que o botao faz o jogador descobre apertando, que e como o
     * botao do Curios ao lado ja funciona.
     */
    private static final class ArmorButton extends Button {
        private static final int SIZE = 10;

        private final AbstractContainerScreen<?> screen;
        private final int relX;
        private final int relY;

        private ArmorButton(AbstractContainerScreen<?> screen, Button original) {
            super(original.getX(), original.getY(), SIZE, SIZE, Component.empty(),
                    ignored -> original.onPress(), DEFAULT_NARRATION);
            this.screen = screen;
            // No criativo o Cosmetic Armor nasce o botao na posicao crua da configuracao e so
            // empurra ele para a borda do inventario no primeiro quadro desenhado — o que ainda
            // nao aconteceu aqui. Nas outras telas ele ja nasce somado. Ler os dois do mesmo jeito
            // poria o botao do criativo a uma tela inteira de distancia do lugar certo.
            this.relX = screen instanceof CreativeModeInventoryScreen
                    ? original.getX()
                    : original.getX() - screen.getGuiLeft();
            this.relY = original.getY() - screen.getGuiTop();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (screen instanceof CreativeModeInventoryScreen creative) {
                // No criativo ele so vale na aba do inventario. Desligado, nao desenha e nao clica.
                active = creative.isInventoryOpen();
                if (!active) {
                    return;
                }
            }
            setX(screen.getGuiLeft() + relX);
            setY(screen.getGuiTop() + relY);
            graphics.blitSprite(isHoveredOrFocused() ? ARMOR_HIGHLIGHTED : ARMOR,
                    getX(), getY(), getWidth(), getHeight());
        }
    }

    private CosmeticArmorButtons() {}
}
