package com.nexussmp.nexuscore.vampire.client;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.quest.client.QuestBookButton;
import de.teamlapen.vampirism.client.gui.screens.VampirismContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Os botoes da tela do Vampirism com a moldura de botao do Minecraft de hoje.
 *
 * <p>Na tela do vampiro ficam tres (quatro para o lorde): habilidades, aparencia e as duas de
 * configurar acoes. Todos usam uma moldura antiga do proprio mod — quadrado preto chapado, que
 * clareia inteiro para branco ao passar o mouse. Ao lado do livro de receitas, que e o botao claro
 * com quina arredondada que o jogo usa hoje, eles destoam.
 *
 * <p>A troca aqui e so da moldura; o que cada botao faz continua sendo do Vampirism, chamado pelo
 * botao original guardado fora da tela — o mesmo arranjo do {@link VampireSkillButton}.
 *
 * <p><b>Os icones.</b> Os arquivos do Vampirism nao servem: nao sao icones, sao <b>botoes
 * inteiros</b>, 100% opacos, com a moldura antiga pintada dentro da imagem. Recortar o miolo foi
 * tentado e descartado — a engrenagem e a gengiva sao cinza-claro, desenhadas para fundo escuro, e
 * somem sobre a moldura clara. Entao:
 * <ul>
 *   <li>habilidades usa a presa como item, igual ao botao do inventario, que leva ao mesmo lugar;</li>
 *   <li>aparencia e as duas de configurar usam icones redesenhados em
 *       {@code textures/gui/sprites/vampire/}, limpos e com fundo transparente.</li>
 * </ul>
 * O estado de mouse em cima fica por conta da moldura, que azula — como no livro de receitas.
 */
public final class VampireMenuButtons {

    private static final String VAMPIRISM = "vampirism";
    private static final int WIDTH = 20;
    /** A altura do botao do livro de receitas. Os do Vampirism tem 20; o nosso centraliza nos 20. */
    private static final int HEIGHT = 18;

    private static final WidgetSprites FRAME =
            new WidgetSprites(QuestBookButton.BUTTON, QuestBookButton.BUTTON_HIGHLIGHTED);

    /** Marcador: este nao tem sprite, desenha a presa do {@link VampireSkillButton}. */
    private static final ResourceLocation SKILLS = NexusCore.id("vampire/skills");
    private static final ResourceLocation APPEARANCE = NexusCore.id("vampire/appearance");
    private static final ResourceLocation ACTIONS = NexusCore.id("vampire/actions");

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof VampirismContainerScreen)
                || !CommonConfig.isModuleEnabled(ModuleIds.VAMPIRE)) {
            return;
        }
        // Copia antes de mexer: a lista do evento e a propria lista de widgets da tela.
        for (GuiEventListener listener : List.copyOf(event.getListenersList())) {
            if (!(listener instanceof ImageButton button)) {
                continue;
            }
            ResourceLocation icon = iconFor(button);
            if (icon == null) {
                continue;
            }
            event.removeListener(button);
            event.addListener(new MenuButton(button, icon));
        }
    }

    /**
     * O icone que substitui a arte daquele botao, ou {@code null} se ele nao e um dos nossos.
     *
     * <p>A escolha e pela arte que o botao desenha, nao pela posicao: posicao muda com a versao,
     * arte nao. Os botoes pequenos da tela — o X que apaga um refinamento, a bussola do mestre de
     * tarefas — caem no {@code null} e ficam como estao: sao de 5 e 8 pixels, nao levam moldura.
     */
    @Nullable
    private static ResourceLocation iconFor(ImageButton button) {
        ResourceLocation sprite = VampireSkillButton.spriteOf(button);
        if (sprite == null || !VAMPIRISM.equals(sprite.getNamespace())) {
            return null;
        }
        return switch (sprite.getPath()) {
            case "widget/skills" -> SKILLS;
            case "widget/appearance" -> APPEARANCE;
            // As duas de configurar (acoes e tarefas de lacaio) usam a mesma engrenagem no mod.
            case "widget/settings" -> ACTIONS;
            default -> null;
        };
    }

    /**
     * O substituto. Fica no lugar do original, um pixel mais abaixo para centralizar os 18 de
     * altura no espaco de 20 que o botao dele ocupava.
     *
     * <p>Esta tela nao anda — nao tem livro de receitas para empurrar nada — entao a posicao e
     * calculada uma vez, ao contrario do botao do inventario.
     */
    private static final class MenuButton extends ImageButton {
        private final ResourceLocation icon;

        private MenuButton(ImageButton original, ResourceLocation icon) {
            super(original.getX(), original.getY() + 1, WIDTH, HEIGHT, FRAME,
                    ignored -> original.onPress(), Component.empty());
            this.icon = icon;
            Tooltip tooltip = VampireSkillButton.tooltipOf(original);
            if (tooltip != null) {
                setTooltip(tooltip);
            }
            // O mod esconde o de tarefas de lacaio para quem nao e lorde, e o de aparencia para
            // quem nao e vampiro. Quem decide continua sendo ele.
            visible = original.visible;
            active = original.active;
        }

        /** Nao chama o {@code super}: a moldura e o icone sao desenhados aqui, como nos vizinhos. */
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blitSprite(isHoveredOrFocused() ? QuestBookButton.BUTTON_HIGHLIGHTED : QuestBookButton.BUTTON,
                    getX(), getY(), getWidth(), getHeight());
            if (icon == SKILLS) {
                // O mesmo desenho do botao do inventario, que leva ao mesmo lugar: a presa, como item.
                graphics.renderFakeItem(VampireSkillButton.fang(), getX() + 2, getY() + 1);
            } else {
                graphics.blitSprite(icon, getX() + 2, getY() + 1, 16, 16);
            }
        }
    }

    private VampireMenuButtons() {}
}
