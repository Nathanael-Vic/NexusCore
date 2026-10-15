package com.nexussmp.nexuscore.display.client.render;

import com.nexussmp.nexuscore.display.PanelPage;
import com.nexussmp.nexuscore.display.block.PanelBlock;
import com.nexussmp.nexuscore.display.blockentity.PanelBlockEntity;
import com.nexussmp.nexuscore.display.client.DisplayClientSettings;
import com.nexussmp.nexuscore.stats.client.ClientStatsCache;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.Objects;

/**
 * O que o cliente guarda por tela: o layout pronto e a navegacao **deste** jogador.
 *
 * <p>A navegacao e local: cada um olha a pagina que quiser na mesma tela, sem pacote de rede e sem
 * mudar o que os outros veem. Parado, a tela volta a alternar sozinha.
 *
 * <p>O layout so e refeito quando algo que aparece nele muda: estatisticas novas, pagina, dimensao,
 * tamanho, idioma, ou o minuto (por causa do "ha 2 min"). No frame a frame e so comparar campos.
 */
public final class PanelClientCache {
    /** Tempo parado ate a tela voltar a alternar sozinha. */
    private static final long IDLE_MILLIS = 20_000L;
    /** Intervalo da alternancia automatica. */
    private static final long AUTO_MILLIS = 10_000L;

    private PanelLayout layout;
    private int statsVersion = -1;
    private int resourceEpoch = -1;
    private PanelPage layoutPage;
    private ResourceLocation dimension;
    private int width;
    private int height;
    private long minute = -1;
    private int layoutPageIndex = -1;
    private int layoutPageTotal = -1;

    private PanelPage page;
    private int slide;
    private boolean jumpToLastSlide;
    private long lastInteraction;
    private long lastAdvance;

    private AABB renderBox;
    private BlockPos boxPos;
    private Direction boxFacing;
    private int boxWidth;
    private int boxHeight;

    public static PanelClientCache of(PanelBlockEntity panel) {
        if (panel.clientCache instanceof PanelClientCache cache) {
            return cache;
        }
        PanelClientCache cache = new PanelClientCache();
        panel.clientCache = cache;
        return cache;
    }

    /** A tela a desenhar agora, ja com a alternancia automatica aplicada. */
    PanelLayout.Page resolve(PanelBlockEntity panel, Font font) {
        ensurePage(panel);
        PanelLayout current = build(panel, font);
        long now = Util.getMillis();
        if (now - lastInteraction > IDLE_MILLIS && now - lastAdvance > AUTO_MILLIS) {
            lastAdvance = now;
            step(panel, current.slideCount(), 1);
            current = build(panel, font);
        }
        return current.slide(slide);
    }

    float unitsPerBlock() {
        return layout.unitsPerBlock();
    }

    /** Clique na tela: borda esquerda volta, borda direita avanca, meio troca de pagina. */
    public void click(PanelBlockEntity panel, double horizontalFraction) {
        ensurePage(panel);
        long now = Util.getMillis();
        lastInteraction = now;
        lastAdvance = now;
        int slides = layout == null ? 1 : layout.slideCount();
        if (horizontalFraction < 0.25) {
            step(panel, slides, -1);
        } else if (horizontalFraction > 0.75) {
            step(panel, slides, 1);
        } else {
            page = PanelPage.next(page, panel.enabledPages(), 1);
            slide = 0;
        }
    }

    private void ensurePage(PanelBlockEntity panel) {
        if (page == null || !panel.isEnabled(page)) {
            page = panel.page();
            slide = 0;
            long now = Util.getMillis();
            lastInteraction = now;
            lastAdvance = now;
        }
    }

    /** Avanca uma tela; passando da ultima (ou antes da primeira), muda de pagina. */
    private void step(PanelBlockEntity panel, int slides, int direction) {
        int next = slide + direction;
        if (next >= slides) {
            page = PanelPage.next(page, panel.enabledPages(), 1);
            slide = 0;
        } else if (next < 0) {
            page = PanelPage.next(page, panel.enabledPages(), -1);
            slide = 0;
            jumpToLastSlide = true;
        } else {
            slide = next;
        }
    }

    private PanelLayout build(PanelBlockEntity panel, Font font) {
        int version = ClientStatsCache.version();
        int epoch = DisplayClientSettings.resourceEpoch();
        int pageTotal = PanelPage.count(panel.enabledPages());
        int pageIndex = PanelPage.indexIn(page, panel.enabledPages());
        boolean clock = page == PanelPage.SUMMARY || page == PanelPage.RECENT;
        long now = System.currentTimeMillis();
        long currentMinute = clock ? now / 60_000L : 0L;

        if (layout == null || version != statsVersion || epoch != resourceEpoch || page != layoutPage
                || !Objects.equals(panel.dimension(), dimension) || panel.width() != width || panel.height() != height
                || currentMinute != minute || pageIndex != layoutPageIndex || pageTotal != layoutPageTotal) {
            statsVersion = version;
            resourceEpoch = epoch;
            layoutPage = page;
            dimension = panel.dimension();
            width = panel.width();
            height = panel.height();
            minute = currentMinute;
            layoutPageIndex = pageIndex;
            layoutPageTotal = pageTotal;
            layout = PanelLayoutBuilder.build(font, ClientStatsCache.received(), ClientStatsCache.get(dimension),
                    page, pageIndex, pageTotal, width, height, now);
        }
        if (jumpToLastSlide) {
            jumpToLastSlide = false;
            slide = layout.slideCount() - 1;
        }
        slide = Math.floorMod(slide, layout.slideCount());
        return layout;
    }

    AABB renderBox(PanelBlockEntity panel, Direction facing) {
        BlockPos pos = panel.getBlockPos();
        if (renderBox == null || !pos.equals(boxPos) || facing != boxFacing
                || panel.width() != boxWidth || panel.height() != boxHeight) {
            boxPos = pos;
            boxFacing = facing;
            boxWidth = panel.width();
            boxHeight = panel.height();
            BlockPos far = pos.relative(PanelBlock.screenRight(facing), boxWidth - 1).above(boxHeight - 1);
            renderBox = AABB.encapsulatingFullBlocks(pos, far).inflate(0.1);
        }
        return renderBox;
    }
}
