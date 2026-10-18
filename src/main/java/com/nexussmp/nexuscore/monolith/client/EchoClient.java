package com.nexussmp.nexuscore.monolith.client;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.client.ScreenNoise;
import com.nexussmp.nexuscore.monolith.Echo;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.List;

/**
 * O eco visto de dentro: o que o cliente faz com a intensidade que o servidor manda.
 *
 * <ul>
 *   <li><b>A tela</b> ganha, de vez em quando, uma linha roxa fina que escorrega para o lado, e um
 *       tom roxo bem leve nas bordas. Tudo sutil: e para incomodar, nao para atrapalhar.</li>
 *   <li><b>O F3</b> embaralha as coordenadas: piscando no "errado", sempre no "quebrado".</li>
 *   <li><b>Os mapas</b> do Xaero se desligam no "quebrado" ({@code MonolithClientModule}).</li>
 *   <li><b>As criaturas</b> marcadas pelo servidor se desenham com a textura faltando
 *       ({@code LivingEntityRendererEchoMixin}).</li>
 * </ul>
 */
public final class EchoClient {
    /** O roxo do Nexus. */
    private static final int PURPLE = 0x9B5CFF;
    private static final int PURPLE_LIGHT = 0xD8B4FF;
    /** A textura roxa e preta das criaturas. */
    public static final ResourceLocation NULL_TEXTURE = NexusCore.id("textures/entity/null.png");

    private static float target;
    private static float shown;
    private static final IntSet GLITCHED = new IntOpenHashSet();

    public static void setIntensity(float value) {
        target = Mth.clamp(value, 0.0F, 1.0F);
    }

    public static void markGlitched(int entityId) {
        GLITCHED.add(entityId);
    }

    public static boolean glitched(Entity entity) {
        return !GLITCHED.isEmpty() && GLITCHED.contains(entity.getId());
    }

    public static float intensity() {
        return shown;
    }

    public static boolean broken() {
        return shown > Echo.WRONG;
    }

    static void onClientTick(ClientTickEvent.Post event) {
        // Sobe e desce devagar: ninguem percebe a hora exata em que comecou.
        shown += (target - shown) * 0.05F;
        if (Math.abs(target - shown) < 0.002F) {
            shown = target;
        }
    }

    static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            GLITCHED.remove(event.getEntity().getId());
        }
    }

    static void reset() {
        target = 0.0F;
        shown = 0.0F;
        GLITCHED.clear();
    }

    static void onRenderGui(RenderGuiEvent.Post event) {
        float f = shown;
        if (f < 0.05F || Minecraft.getInstance().options.hideGui) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        long frame = ScreenNoise.frame(Util.getMillis());
        RandomSource random = ScreenNoise.random(frame);

        // As bordas: um roxo que quase nao se ve, mais presente quanto mais perto.
        int edge = (int) (f * 38.0F) << 24;
        int band = height / 5;
        graphics.fillGradient(0, 0, width, band, edge | PURPLE, PURPLE);
        graphics.fillGradient(0, height - band, width, height, PURPLE, edge | PURPLE);

        // As linhas: finas, raras, e somem no quadro seguinte.
        if (random.nextFloat() < 0.03F + 0.12F * f) {
            int count = 1 + random.nextInt(1 + (int) (f * 2.0F));
            for (int i = 0; i < count; i++) {
                int y = random.nextInt(height);
                int shift = random.nextInt(13) - 6;
                int alpha = 25 + random.nextInt(20 + (int) (f * 60.0F));
                int color = random.nextFloat() < 0.3F ? PURPLE_LIGHT : PURPLE;
                graphics.fill(shift, y, width + shift, y + 1, (alpha << 24) | color);
            }
        }
        // No "quebrado", de vez em quando uma faixa um pouco mais grossa.
        if (f > Echo.WRONG && random.nextFloat() < 0.02F + 0.04F * f) {
            int y = random.nextInt(height);
            int thickness = 2 + random.nextInt(4);
            graphics.fill(0, y, width, y + thickness, ((18 + random.nextInt(30)) << 24) | PURPLE);
        }
    }

    /** As coordenadas do F3 viram caracteres que nao param de trocar. */
    static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        float f = shown;
        if (f <= Echo.STRANGE) {
            return;
        }
        boolean scramble = f > Echo.WRONG || ScreenNoise.random(ScreenNoise.frame(Util.getMillis())).nextFloat() < 0.15F;
        if (!scramble) {
            return;
        }
        List<String> left = event.getLeft();
        for (int i = 0; i < left.size(); i++) {
            String line = left.get(i);
            for (String prefix : new String[] {"XYZ: ", "Block: ", "Chunk: ", "Facing: "}) {
                if (line.startsWith(prefix)) {
                    left.set(i, prefix + "§k" + line.substring(prefix.length()) + "§r");
                    break;
                }
            }
        }
    }

    private EchoClient() {}
}
