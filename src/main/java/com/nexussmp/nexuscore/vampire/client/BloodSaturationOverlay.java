package com.nexussmp.nexuscore.vampire.client;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.client.VIngameOverlays;
import de.teamlapen.vampirism.modcompat.IMCHandler;
import de.teamlapen.vampirism.util.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.HUDOverlayEvent;
import squeek.appleskin.client.HUDOverlayHandler;

import java.text.DecimalFormat;

/**
 * O contorno amarelo do AppleSkin no formato das gotas de sangue.
 *
 * <p>Quando alguem vira vampiro, o Vampirism esconde a barra de fome e desenha as gotas no mesmo
 * lugar. O AppleSkin nao sabe disso: continua desenhando por cima o contorno de saturacao no
 * formato das carnes — e com a saturacao da fome comum, que para o vampiro nao quer dizer nada (o
 * Vampirism prende a fome dele em 10 e usa o sangue no lugar).
 *
 * <p>Aqui o desenho dele e cancelado pela propria API do AppleSkin, e o nosso entra no lugar: o
 * mesmo contorno, com a mesma paleta amarela, so que seguindo a borda da gota e contando a
 * saturacao do <b>sangue</b>. Os quatro niveis parciais sobem de baixo para cima, como liquido
 * enchendo a gota. A regra de qual gota recebe qual nivel e a do AppleSkin, copiada como esta.
 *
 * <p><b>Quando desenhar.</b> Logo depois das gotas, e nao na camada do AppleSkin. O contorno fica em
 * cima da borda preta da gota, pixel por pixel: desenhado antes dela, some inteiro. E a ordem entre
 * a camada do AppleSkin e a do Vampirism sai de qual mod registra primeiro — coisa que ninguem aqui
 * controla. O jogo avisa depois de cada camada ({@link RenderGuiLayerEvent.Post}); ouvindo o aviso
 * da barra de sangue, o contorno cai sempre por cima, em qualquer ordem de mods. Quem decide se ele
 * aparece continuam sendo as opcoes do AppleSkin, conferidas do mesmo jeito que ele confere.
 *
 * <p>Tambem sai, para o vampiro, a previa piscando de quanto a comida na mao encheria a barra:
 * sao coxinhas de frango desenhadas por cima das gotas, e comida nao enche sangue. E entra na tela
 * do F3, embaixo da linha de fome do AppleSkin, uma linha igual para o sangue.
 *
 * <p>Esta classe fala com o AppleSkin e com o Vampirism, entao so e carregada com os dois
 * instalados — ver {@link VampireClientModule}.
 */
public final class BloodSaturationOverlay {
    private static final ResourceLocation QUARTER = NexusCore.id("vampire/blood_saturation_quarter");
    private static final ResourceLocation HALF = NexusCore.id("vampire/blood_saturation_half");
    private static final ResourceLocation THREE_QUARTER = NexusCore.id("vampire/blood_saturation_three_quarter");
    private static final ResourceLocation FULL = NexusCore.id("vampire/blood_saturation_full");

    private static final int ICON = 9;
    /** Uma gota a cada 8 pixels, da direita para a esquerda — como as carnes e como o Vampirism. */
    private static final int STEP = 8;
    /** Dez gotas de dois pontos. O AppleSkin tambem para em 20. */
    private static final float MAX_SATURATION = 20.0F;
    private static final DecimalFormat SATURATION_FORMAT = new DecimalFormat("#.##");

    /** A altura ocupada na direita logo antes da barra de sangue; -1 fora dela. */
    private static int rightHeightBefore = -1;

    static void register(IEventBus game) {
        game.addListener(HUDOverlayEvent.Saturation.class, BloodSaturationOverlay::onSaturation);
        game.addListener(HUDOverlayEvent.HungerRestored.class, BloodSaturationOverlay::onHungerRestored);
        game.addListener(RenderGuiLayerEvent.Pre.class, BloodSaturationOverlay::beforeLayer);
        game.addListener(RenderGuiLayerEvent.Post.class, BloodSaturationOverlay::afterLayer);
        game.addListener(CustomizeGuiOverlayEvent.DebugText.class, BloodSaturationOverlay::onDebugText);
    }

    /** O contorno de carne do AppleSkin, que cairia por cima das gotas: sai. */
    private static void onSaturation(HUDOverlayEvent.Saturation event) {
        if (bloodBarShown()) {
            event.setCanceled(true);
        }
    }

    private static void onHungerRestored(HUDOverlayEvent.HungerRestored event) {
        if (bloodBarShown()) {
            event.setCanceled(true);
        }
    }

    private static void beforeLayer(RenderGuiLayerEvent.Pre event) {
        if (VIngameOverlays.BLOOD_BAR_ID.equals(event.getName())) {
            rightHeightBefore = Minecraft.getInstance().gui.rightHeight;
        }
    }

    private static void afterLayer(RenderGuiLayerEvent.Post event) {
        if (!VIngameOverlays.BLOOD_BAR_ID.equals(event.getName()) || off()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int before = rightHeightBefore;
        rightHeightBefore = -1;
        // A barra de sangue abre espaco para si quando se desenha. Se a altura nao mudou, ela nao
        // desenhou nada neste quadro — e nao ha gota para contornar.
        if (before < 0 || minecraft.gui.rightHeight == before || !appleSkinShowsSaturation(minecraft)) {
            return;
        }
        float saturation = VampireClient.bloodSaturation();
        if (Float.isNaN(saturation) || saturation <= 0.0F) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        // As mesmas contas que o Vampirism usa para por as gotas.
        int right = graphics.guiWidth() / 2 + 91;
        int top = graphics.guiHeight() - before;
        float value = Math.min(saturation, MAX_SATURATION);
        int drops = Mth.ceil(value / 2.0F);
        for (int i = 0; i < drops; i++) {
            float fill = value / 2.0F - i;
            ResourceLocation sprite = fill >= 1.0F ? FULL
                    : fill > 0.5F ? THREE_QUARTER
                    : fill > 0.25F ? HALF
                    : QUARTER;
            graphics.blitSprite(sprite, right - i * STEP - ICON, top, ICON, ICON);
        }
    }

    /** A linha do sangue no F3, no formato da linha de fome que o AppleSkin poe la. */
    private static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (off() || minecraft.player == null || !minecraft.getDebugOverlay().showDebugScreen()
                || !ModConfig.SHOW_FOOD_DEBUG_INFO.get() || !Helper.isVampire(minecraft.player)) {
            return;
        }
        float saturation = VampireClient.bloodSaturation();
        event.getLeft().add("blood: " + VampirismAPI.vampirePlayer(minecraft.player).getBloodLevel()
                + ", blood sat: " + (Float.isNaN(saturation) ? "?" : SATURATION_FORMAT.format(saturation)));
    }

    /**
     * Se o AppleSkin mostraria o contorno agora — as mesmas condicoes que ele confere antes de
     * desenhar o dele: a tela nao esta escondida, a pessoa pode tomar dano, nao esta montada num
     * bicho (ai a barra vira a vida do bicho) e a opcao do contorno esta ligada.
     */
    private static boolean appleSkinShowsSaturation(Minecraft minecraft) {
        return minecraft.player != null
                && !minecraft.options.hideGui
                && minecraft.gameMode != null
                && minecraft.gameMode.canHurtPlayer()
                && !HUDOverlayHandler.isMountHealthShown(minecraft.player)
                && ModConfig.SHOW_SATURATION_OVERLAY.get();
    }

    /**
     * Se as gotas estao na tela agora. As mesmas condicoes que o Vampirism usa para trocar a barra:
     * quando alguma delas falha, a barra de fome comum volta, e o contorno de carne volta a estar certo.
     */
    private static boolean bloodBarShown() {
        Minecraft minecraft = Minecraft.getInstance();
        return !off()
                && minecraft.player != null
                && minecraft.gameMode != null
                && minecraft.gameMode.hasExperience()
                && !IMCHandler.requestedToDisableBloodbar
                && Helper.isVampire(minecraft.player);
    }

    /** Modulo desligado na config: o AppleSkin volta a ser o de sempre. */
    private static boolean off() {
        return !CommonConfig.isModuleEnabled(ModuleIds.VAMPIRE);
    }

    private BloodSaturationOverlay() {}
}
