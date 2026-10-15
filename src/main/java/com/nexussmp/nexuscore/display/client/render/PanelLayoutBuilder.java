package com.nexussmp.nexuscore.display.client.render;

import com.nexussmp.nexuscore.display.PanelPage;
import com.nexussmp.nexuscore.stats.ExplorationNames;
import com.nexussmp.nexuscore.stats.data.RecentDiscovery;
import com.nexussmp.nexuscore.stats.network.DimensionStatsView;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Monta o {@link PanelLayout} de uma tela: mede textos, quebra listas em telas, corta o que nao cabe.
 *
 * <p>So aparece o que ja foi descoberto. A dimensao e desconhecida: nao existe total, barra de progresso
 * nem espaco reservado para o que falta.
 */
final class PanelLayoutBuilder {
    private static final float BASE_UNITS_PER_BLOCK = 120F;
    private static final int LINE = 10;
    private static final int PADDING = 5;
    private static final int GAP = 6;
    private static final int ARROW_SPACE = 8;
    private static final int DOT_SIZE = 3;
    private static final int DOT_GAP = 3;
    /** Abaixo disto a tela e estreita demais para as setas (uma tela 1x1 fica sem elas). */
    private static final int MIN_WIDTH_FOR_ARROWS = 110;
    private static final FormattedText ELLIPSIS = FormattedText.of("...");
    private static final Component ARROW_LEFT = Component.literal("<");
    private static final Component ARROW_RIGHT = Component.literal(">");

    private final Font font;
    private final DimensionStatsView view;
    private final long now;
    private final float units;
    private final int pageIndex;
    private final int pageTotal;
    private final int frameLeft;
    private final int frameRight;
    private final int left;
    private final int top;
    private final int right;
    private final int bottom;
    private final boolean arrows;

    private final List<PanelLayout.Page> pages = new ArrayList<>();
    private final FloatArrayList rects = new FloatArrayList();
    private final IntArrayList rectColors = new IntArrayList();
    private final List<FormattedCharSequence> texts = new ArrayList<>();
    private final FloatArrayList textX = new FloatArrayList();
    private final FloatArrayList textY = new FloatArrayList();
    private final IntArrayList textColors = new IntArrayList();

    private PanelLayoutBuilder(Font font, @Nullable DimensionStatsView view, int pageIndex, int pageTotal,
                               int widthBlocks, int heightBlocks, long now) {
        this.font = font;
        this.view = view;
        this.now = now;
        this.pageIndex = pageIndex;
        this.pageTotal = pageTotal;
        // Telas maiores usam letra maior, para continuar legivel de longe.
        this.units = BASE_UNITS_PER_BLOCK / (1F + 0.5F * (Math.min(widthBlocks, heightBlocks) - 1));
        int bezel = Math.round(units / 16F);
        this.frameLeft = bezel + 2;
        this.frameRight = Math.round(widthBlocks * units) - bezel - 2;
        this.arrows = frameRight - frameLeft >= MIN_WIDTH_FOR_ARROWS;
        int margin = bezel + PADDING + (arrows ? ARROW_SPACE : 0);
        this.left = margin;
        this.right = Math.round(widthBlocks * units) - margin;
        this.top = bezel + PADDING;
        this.bottom = Math.round(heightBlocks * units) - bezel - PADDING - (pageTotal > 1 ? DOT_SIZE + 4 : 0);
    }

    static PanelLayout build(Font font, boolean received, @Nullable DimensionStatsView view, PanelPage page,
                             int pageIndex, int pageTotal, int widthBlocks, int heightBlocks, long nowMillis) {
        PanelLayoutBuilder builder = new PanelLayoutBuilder(font, view, pageIndex, pageTotal,
                widthBlocks, heightBlocks, nowMillis);
        if (!received) {
            builder.message(Component.translatable("nexuscore.panel.waiting"));
        } else if (view == null) {
            builder.message(Component.translatable("nexuscore.panel.not_tracked"));
        } else {
            switch (page) {
                case SUMMARY -> builder.summary();
                case BIOMES -> builder.biomes();
                case STRUCTURES -> builder.structures();
                case RECENT -> builder.recent();
            }
        }
        return builder.finish();
    }

    // ---- paginas ----

    private void message(Component text) {
        FormattedCharSequence line = fit(text, right - left);
        float x = left + (right - left - font.width(line)) / 2F;
        float y = top + (bottom - top - font.lineHeight) / 2F;
        text(line, x, y, PanelColors.LABEL);
        endSlide();
    }

    private void summary() {
        int y = header(ExplorationNames.dimension(view.dimension()), null);
        y = labelValue(y, Component.translatable("nexuscore.panel.biomes"), number(view.biomes().size()));
        y = labelValue(y, Component.translatable("nexuscore.panel.structure_types"), number(view.structureTypes().size()));
        y = labelValue(y, Component.translatable("nexuscore.panel.structures"), number(view.structureCount()));
        y = labelValue(y, Component.translatable("nexuscore.panel.area"), area());

        if (!view.recent().isEmpty() && fits(y + 4 + LINE * 2)) {
            y = separator(y);
            text(fit(Component.translatable("nexuscore.panel.last_discovery"), right - left), left, y, PanelColors.LABEL);
            y += LINE;
            recentLine(y, view.recent().get(0));
        }
        endSlide();
    }

    private void biomes() {
        List<GridEntry> entries = new ArrayList<>();
        for (DimensionStatsView.Entry entry : sorted(view.biomes(), ExplorationNames::biome)) {
            entries.add(new GridEntry(ExplorationNames.biome(entry.id()), null, Component.literal(entry.pioneer())));
        }
        grid(Component.translatable("nexuscore.panel.page.biomes"), number(view.biomes().size()), entries);
    }

    private void structures() {
        List<GridEntry> entries = new ArrayList<>();
        for (DimensionStatsView.Entry entry : sorted(view.structureTypes(), ExplorationNames::structure)) {
            entries.add(new GridEntry(ExplorationNames.structure(entry.id()),
                    Component.literal("×" + entry.count()), Component.literal(entry.pioneer())));
        }
        grid(Component.translatable("nexuscore.panel.page.structures"), number(view.structureTypes().size()), entries);
    }

    private void recent() {
        List<DimensionStatsView.Recent> recent = view.recent();
        if (recent.isEmpty()) {
            header(Component.translatable("nexuscore.panel.page.recent"), null);
            message(Component.translatable("nexuscore.panel.recent.empty"));
            return;
        }
        int rows = rowsPerSlide();
        int slides = (recent.size() + rows - 1) / rows;
        for (int slide = 0; slide < slides; slide++) {
            int y = header(Component.translatable("nexuscore.panel.page.recent"), slideIndicator(slide, slides));
            for (int i = slide * rows; i < Math.min(recent.size(), (slide + 1) * rows) && fits(y + LINE); i++) {
                y = recentLine(y, recent.get(i));
            }
            endSlide();
        }
    }

    // ---- blocos de layout ----

    /** Titulo a esquerda, contador/indicador a direita e linha separadora. Devolve o y do conteudo. */
    private int header(Component title, @Nullable Component indicator) {
        int titleWidth = right - left;
        if (indicator != null) {
            FormattedCharSequence indicatorLine = sequence(indicator);
            int indicatorWidth = font.width(indicatorLine);
            text(indicatorLine, right - indicatorWidth, top, PanelColors.MUTED);
            titleWidth -= indicatorWidth + GAP;
        }
        text(fit(title, titleWidth), left, top, PanelColors.TITLE);
        return separator(top + LINE);
    }

    private int separator(int y) {
        rect(left, y, right, y + 1, PanelColors.SEPARATOR);
        return y + 4;
    }

    private int labelValue(int y, Component label, Component value) {
        if (!fits(y + LINE)) {
            return y;
        }
        FormattedCharSequence valueLine = sequence(value);
        int valueWidth = font.width(valueLine);
        text(valueLine, right - valueWidth, y, PanelColors.VALUE);
        text(fit(label, right - left - valueWidth - GAP), left, y, PanelColors.LABEL);
        return y + LINE;
    }

    /** "Fulano descobriu Deep Dark" a esquerda, "ha 2 h" a direita. */
    private int recentLine(int y, DimensionStatsView.Recent entry) {
        FormattedCharSequence time = sequence(ago(entry.epochMillis()));
        int timeWidth = font.width(time);
        text(time, right - timeWidth, y, PanelColors.MUTED);

        Component player = Component.literal(entry.playerName()).withStyle(Style.EMPTY.withColor(PanelColors.PIONEER & 0xFFFFFF));
        Component name = discoveryName(entry).copy().withStyle(Style.EMPTY.withColor(PanelColors.VALUE & 0xFFFFFF));
        String key = switch (entry.kind()) {
            case BIOME -> "nexuscore.panel.recent.biome";
            case STRUCTURE_TYPE -> "nexuscore.panel.recent.structure_type";
            case STRUCTURE -> "nexuscore.panel.recent.structure";
        };
        text(fit(Component.translatable(key, player, name), right - left - timeWidth - GAP), left, y, PanelColors.LABEL);
        return y + LINE;
    }

    private record GridEntry(Component name, @Nullable Component count, @Nullable Component pioneer) {}

    /** Lista em colunas do que ja foi descoberto. */
    private void grid(Component title, Component total, List<GridEntry> entries) {
        if (entries.isEmpty()) {
            header(title, null);
            message(Component.translatable("nexuscore.panel.recent.empty"));
            return;
        }
        int innerWidth = right - left;
        int natural = 0;
        for (GridEntry entry : entries) {
            natural = Math.max(natural, entryWidth(entry));
        }
        int columns = Math.max(1, (innerWidth + GAP) / (Math.min(innerWidth, Math.max(natural, 30)) + GAP));
        int columnWidth = (innerWidth - GAP * (columns - 1)) / columns;
        int rows = rowsPerSlide();
        int perSlide = rows * columns;
        int slides = Math.max(1, (entries.size() + perSlide - 1) / perSlide);

        for (int slide = 0; slide < slides; slide++) {
            Component indicator = slides > 1
                    ? Component.empty().append(total).append("  ").append(slideIndicator(slide, slides))
                    : total;
            int y = header(title, indicator);
            for (int i = 0; i < perSlide && slide * perSlide + i < entries.size(); i++) {
                int row = i / columns;
                int x = left + (i % columns) * (columnWidth + GAP);
                int rowY = y + row * LINE;
                if (fits(rowY + LINE)) {
                    cell(entries.get(slide * perSlide + i), x, rowY, columnWidth);
                }
            }
            endSlide();
        }
    }

    private int entryWidth(GridEntry entry) {
        int width = font.width(entry.name());
        if (entry.count() != null) {
            width += 3 + font.width(entry.count());
        }
        if (entry.pioneer() != null) {
            width += GAP + font.width(entry.pioneer());
        }
        return width;
    }

    private void cell(GridEntry entry, int x, int y, int width) {
        int countWidth = entry.count() == null ? 0 : font.width(entry.count()) + 3;
        int pioneerWidth = entry.pioneer() == null ? 0 : font.width(entry.pioneer()) + GAP;
        // Sem espaco: o pioneiro sai antes do nome ficar ilegivel.
        if (width - countWidth - pioneerWidth < 30) {
            pioneerWidth = 0;
        }
        FormattedCharSequence name = fit(entry.name(), width - countWidth - pioneerWidth);
        text(name, x, y, PanelColors.VALUE);
        if (entry.count() != null) {
            text(sequence(entry.count()), x + font.width(name) + 3, y, PanelColors.MUTED);
        }
        if (pioneerWidth > 0) {
            FormattedCharSequence pioneer = fit(entry.pioneer(), pioneerWidth - GAP);
            text(pioneer, x + width - font.width(pioneer), y, PanelColors.PIONEER);
        }
    }

    /** Setas nas bordas e bolinhas da pagina: iguais em todas as telas da mesma pagina. */
    private void decorate() {
        if (arrows) {
            float y = top + (bottom - top - font.lineHeight) / 2F;
            text(sequence(ARROW_LEFT), frameLeft, y, PanelColors.MUTED);
            text(sequence(ARROW_RIGHT), frameRight - font.width(ARROW_RIGHT), y, PanelColors.MUTED);
        }
        if (pageTotal > 1) {
            float totalWidth = pageTotal * DOT_SIZE + (pageTotal - 1) * DOT_GAP;
            float x = frameLeft + (frameRight - frameLeft - totalWidth) / 2F;
            float y = bottom + 4;
            for (int i = 0; i < pageTotal; i++) {
                rect(x, y, x + DOT_SIZE, y + DOT_SIZE, i == pageIndex ? PanelColors.VALUE : PanelColors.SEPARATOR);
                x += DOT_SIZE + DOT_GAP;
            }
        }
    }

    // ---- utilitarios ----

    private int rowsPerSlide() {
        return Math.max(1, (bottom - top - (LINE + 4)) / LINE);
    }

    private List<DimensionStatsView.Entry> sorted(List<DimensionStatsView.Entry> entries,
                                                 java.util.function.Function<net.minecraft.resources.ResourceLocation, Component> namer) {
        List<DimensionStatsView.Entry> copy = new ArrayList<>(entries);
        copy.sort(Comparator.comparing(entry -> namer.apply(entry.id()).getString()));
        return copy;
    }

    private Component discoveryName(DimensionStatsView.Recent entry) {
        return entry.kind() == RecentDiscovery.Kind.BIOME
                ? ExplorationNames.biome(entry.id())
                : ExplorationNames.structure(entry.id());
    }

    private Component area() {
        if (view.areaRadius() == 0) {
            return Component.translatable("nexuscore.panel.area.unlimited");
        }
        long side = 2L * view.areaRadius();
        return Component.literal(side + " × " + side);
    }

    private Component ago(long epochMillis) {
        long seconds = Math.max(0L, now - epochMillis) / 1000L;
        if (seconds < 60L) {
            return Component.translatable("nexuscore.time.now");
        }
        if (seconds < 3600L) {
            return Component.translatable("nexuscore.time.minutes", seconds / 60L);
        }
        if (seconds < 86400L) {
            return Component.translatable("nexuscore.time.hours", seconds / 3600L);
        }
        return Component.translatable("nexuscore.time.days", seconds / 86400L);
    }

    private static Component number(int value) {
        return Component.literal(Integer.toString(value));
    }

    private static Component slideIndicator(int slide, int count) {
        return Component.literal((slide + 1) + "/" + count);
    }

    private boolean fits(int yBottom) {
        return yBottom <= bottom;
    }

    private FormattedCharSequence sequence(FormattedText text) {
        return Language.getInstance().getVisualOrder(text);
    }

    /** Corta com "..." o que passar da largura. */
    private FormattedCharSequence fit(FormattedText text, int maxWidth) {
        if (maxWidth <= 0) {
            return FormattedCharSequence.EMPTY;
        }
        if (font.width(text) <= maxWidth) {
            return sequence(text);
        }
        FormattedText cut = font.substrByWidth(text, Math.max(0, maxWidth - font.width(ELLIPSIS)));
        return sequence(FormattedText.composite(cut, ELLIPSIS));
    }

    private void text(FormattedCharSequence text, float x, float y, int color) {
        texts.add(text);
        textX.add(x);
        textY.add(y);
        textColors.add(color);
    }

    private void rect(float x0, float y0, float x1, float y1, int color) {
        rects.add(x0);
        rects.add(y0);
        rects.add(x1);
        rects.add(y1);
        rectColors.add(color);
    }

    private void endSlide() {
        decorate();
        pages.add(new PanelLayout.Page(rects.toFloatArray(), rectColors.toIntArray(),
                texts.toArray(new FormattedCharSequence[0]), textX.toFloatArray(), textY.toFloatArray(), textColors.toIntArray()));
        rects.clear();
        rectColors.clear();
        texts.clear();
        textX.clear();
        textY.clear();
        textColors.clear();
    }

    /** Fundo e moldura vem do modelo do bloco (blockstate multipart); aqui so sai o conteudo. */
    private PanelLayout finish() {
        if (pages.isEmpty()) {
            endSlide();
        }
        return new PanelLayout(units, pages.toArray(new PanelLayout.Page[0]));
    }
}
