package com.nexussmp.nexuscore.quest.client;

import com.nexussmp.nexuscore.quest.client.screen.QuestTheme;
import com.nexussmp.nexuscore.quest.network.QuestView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * O corpo do diario: o papel, a lista e a ficha de uma quest.
 *
 * <p>E um widget de verdade, e nao um desenho solto por cima da tela, por um motivo de camada: os
 * botoes do diario sao widgets, e o jogo desenha os widgets na ordem em que entraram. Como corpo
 * entra primeiro, ele fica por baixo — papel embaixo, botoes em cima, que e o que se espera. Desenhar
 * o papel depois (num evento de "render post") cobriria os proprios botoes.
 *
 * <p>Ele tambem e quem sabe onde cada fileira caiu, entao o clique na quest e a rolagem moram aqui,
 * e nao num rastreador de mouse a parte.
 */
final class QuestPanelBody extends AbstractWidget {
    private static final int ROW_HEIGHT = 22;
    private static final int PADDING = 6;

    private final int listTop;
    private final int listHeight;

    QuestPanelBody(int x, int y, int width, int height, int listTop, int listHeight) {
        super(x, y, width, height, Component.translatable("nexuscore.quest.book.title"));
        this.listTop = listTop;
        this.listHeight = listHeight;
    }

    /** A fileira sob o mouse, ou {@code null}. Serve para o clique e para a dica. */
    @Nullable
    QuestView questAt(double mouseX, double mouseY) {
        if (InventoryQuestPanel.selected() != null || mouseX < getX() + PADDING
                || mouseX > getX() + width - PADDING || mouseY < listTop || mouseY > listTop + listHeight) {
            return null;
        }
        List<QuestView> quests = InventoryQuestPanel.currentList();
        int index = (int) ((mouseY - listTop - 3) / ROW_HEIGHT) + InventoryQuestPanel.scroll();
        return index >= 0 && index < quests.size() ? quests.get(index) : null;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        QuestTheme.panel(graphics, getX(), getY(), width, height);
        QuestTheme.text(graphics, font, InventoryQuestPanel.pageTitle(), getX() + 8, getY() + 7, QuestTheme.TITLE);

        if (InventoryQuestPanel.page().isForm()) {
            // A pagina de criar missao e so campos de texto; os rotulos sao desenhados junto deles.
            renderFormLabels(graphics);
            return;
        }

        QuestView detail = InventoryQuestPanel.selectedQuest();
        if (detail != null) {
            renderDetail(graphics, detail);
            return;
        }
        renderList(graphics, mouseX, mouseY);
    }

    // ---- a lista ----

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        int listX = getX() + PADDING;
        int listWidth = width - 2 * PADDING;
        QuestTheme.inset(graphics, listX, listTop, listWidth, listHeight);

        List<QuestView> quests = InventoryQuestPanel.currentList();
        if (quests.isEmpty()) {
            QuestTheme.wrapped(graphics, font, InventoryQuestPanel.emptyMessage(),
                    listX + 4, listTop + 6, listWidth - 8, 4, QuestTheme.MUTED);
            return;
        }

        int visible = Math.max(1, (listHeight - 4) / ROW_HEIGHT);
        int scroll = InventoryQuestPanel.scroll();
        for (int i = 0; i < visible && i + scroll < quests.size(); i++) {
            QuestView quest = quests.get(i + scroll);
            int rowY = listTop + 3 + i * ROW_HEIGHT;
            boolean over = mouseX >= listX && mouseX < listX + listWidth
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (over) {
                graphics.fill(listX + 2, rowY - 1, listX + listWidth - 2, rowY + ROW_HEIGHT - 2, 0x30000000);
            }
            graphics.drawString(font, QuestTheme.fit(font, Component.literal(quest.title()), listWidth - 10),
                    listX + 4, rowY, quest.open() ? QuestTheme.TITLE : QuestTheme.MUTED, false);
            graphics.drawString(font, QuestTheme.fit(font, InventoryQuestPanel.rowLine(quest), listWidth - 10),
                    listX + 4, rowY + 10, InventoryQuestPanel.rowColor(quest), false);
        }

        if (quests.size() > visible) {
            Component counter = Component.literal((scroll + 1) + "-"
                    + Math.min(quests.size(), scroll + visible) + "/" + quests.size());
            graphics.drawString(font, counter, getX() + width - PADDING - font.width(counter), getY() + 7,
                    QuestTheme.MUTED, false);
        }
    }

    // ---- a ficha ----

    /**
     * A quest aberta, com cada coisa no seu lugar.
     *
     * <p>Era tudo uma linha so de texto que so se revelava com o mouse em cima. Aqui titulo, quem
     * pediu, o pedido e a recompensa sao blocos separados, com espaco entre eles — da para ler sem
     * ter de segurar o mouse parado.
     */
    private void renderDetail(GuiGraphics graphics, QuestView quest) {
        var font = Minecraft.getInstance().font;
        int x = getX() + PADDING + 2;
        int inner = width - 2 * PADDING - 4;
        int y = listTop;

        y += QuestTheme.wrapped(graphics, font, Component.literal(quest.title()), x, y, inner, 2, QuestTheme.TITLE);
        y += 2;

        Component from = quest.fromServer()
                ? Component.translatable("nexuscore.quest.entry.server")
                : Component.translatable("nexuscore.quest.details.author", quest.authorName());
        QuestTheme.text(graphics, font, from, x, y, quest.fromServer() ? QuestTheme.ACCENT : QuestTheme.MUTED);
        y += font.lineHeight + 5;

        if (!quest.description().isBlank()) {
            y += QuestTheme.wrapped(graphics, font, Component.literal(quest.description()), x, y, inner, 5,
                    QuestTheme.TEXT);
            y += 4;
        }

        if (!quest.reward().isBlank()) {
            y += QuestTheme.wrapped(graphics, font,
                    Component.translatable("nexuscore.quest.details.reward", quest.reward()),
                    x, y, inner, 3, QuestTheme.ACCENT);
            y += 4;
        }

        QuestTheme.wrapped(graphics, font, InventoryQuestPanel.statusLine(quest), x, y, inner, 3,
                quest.pending() ? QuestTheme.WARNING : QuestTheme.MUTED);
    }

    private void renderFormLabels(GuiGraphics graphics) {
        var font = Minecraft.getInstance().font;
        int x = getX() + PADDING + 2;
        int y = listTop;
        for (Component label : InventoryQuestPanel.formLabels()) {
            QuestTheme.text(graphics, font, label, x, y, QuestTheme.MUTED);
            y += InventoryQuestPanel.FORM_STEP;
        }
    }

    // ---- interacao ----

    /**
     * So fica com o clique que caiu numa fileira.
     *
     * <p>Isto precisa ser dito: o corpo cobre o painel inteiro, inclusive o rodape onde os botoes
     * ficam, e a tela entrega o clique ao <b>primeiro</b> widget que disser que o aceitou. Como o
     * corpo entra antes dos botoes (para ficar por baixo no desenho), aceitar qualquer clique faria
     * os botoes nunca receberem nenhum. Entao ele recusa tudo que nao for fileira, e o clique segue
     * para quem estiver ali.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        QuestView quest = questAt(mouseX, mouseY);
        if (quest == null) {
            return false;
        }
        playDownSound(Minecraft.getInstance().getSoundManager());
        InventoryQuestPanel.openDetail(quest.id());
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY) || InventoryQuestPanel.selected() != null) {
            return false;
        }
        InventoryQuestPanel.scrollBy(-(int) Math.signum(scrollY));
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
