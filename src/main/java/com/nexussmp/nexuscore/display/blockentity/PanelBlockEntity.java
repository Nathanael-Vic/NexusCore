package com.nexussmp.nexuscore.display.blockentity;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.display.PanelPage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Um bloco do painel. Numa tela de varios blocos, so o controlador (canto inferior esquerdo, visto de frente)
 * guarda pagina/dimensao e tamanho; os outros guardam so onde o controlador esta (offset relativo,
 * que continua certo se a tela for copiada com /clone ou estruturas).
 */
public final class PanelBlockEntity extends BlockEntity {
    private static final int MAX_SIZE = 32;

    private PanelPage page = PanelPage.SUMMARY;
    /** Paginas que esta tela mostra; cada jogador navega sozinho entre elas. */
    private int enabledPages = PanelPage.ALL_MASK;
    @Nullable
    private ResourceLocation dimension;
    private int controllerDx;
    private int controllerDy;
    private int controllerDz;
    private int width = 1;
    private int height = 1;

    /** Cache de desenho do cliente (layout, caixa de render). Nunca salvo; {@code Object} para o servidor nao carregar tipos de cliente. */
    public Object clientCache;

    public PanelBlockEntity(BlockPos pos, BlockState state) {
        super(NexusRegistries.EXPLORATION_PANEL_ENTITY.get(), pos, state);
    }

    public boolean isController() {
        return controllerDx == 0 && controllerDy == 0 && controllerDz == 0;
    }

    public BlockPos controllerPos() {
        return worldPosition.offset(controllerDx, controllerDy, controllerDz);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Pagina inicial: a escolhida pelo OP, ou a primeira habilitada se ela foi desligada. */
    public PanelPage page() {
        return isEnabled(page) ? page : PanelPage.next(page, enabledPages, 1);
    }

    public int enabledPages() {
        return enabledPages == 0 ? PanelPage.ALL_MASK : enabledPages;
    }

    public boolean isEnabled(PanelPage candidate) {
        return (enabledPages() & candidate.bit()) != 0;
    }

    public void setEnabledPages(int mask) {
        int safeMask = (mask & PanelPage.ALL_MASK) == 0 ? PanelPage.ALL_MASK : mask & PanelPage.ALL_MASK;
        if (safeMask != enabledPages) {
            enabledPages = safeMask;
            markUpdated();
        }
    }

    /** {@code null} = primeira dimensao rastreada. */
    @Nullable
    public ResourceLocation dimension() {
        return dimension;
    }

    /** O controlador da tela a que este bloco pertence (ele mesmo, se estiver sozinho ou o controlador sumir). */
    @Nullable
    public static PanelBlockEntity controllerOf(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof PanelBlockEntity panel)) {
            return null;
        }
        if (!panel.isController() && level.getBlockEntity(panel.controllerPos()) instanceof PanelBlockEntity controller
                && controller.isController()) {
            return controller;
        }
        return panel;
    }

    public void setPage(PanelPage page) {
        if (this.page != page) {
            this.page = page;
            markUpdated();
        }
    }

    public void setDimension(@Nullable ResourceLocation dimension) {
        if (dimension == null ? this.dimension != null : !dimension.equals(this.dimension)) {
            this.dimension = dimension;
            markUpdated();
        }
    }

    public void copySettingsFrom(PanelBlockEntity other) {
        setPage(other.page);
        setDimension(other.dimension);
        setEnabledPages(other.enabledPages);
    }

    /** Define a posicao na tela. Chamado pelo multibloco; so envia atualizacao se algo mudou. */
    public void applyMultiblock(int dx, int dy, int dz, int width, int height) {
        width = Mth.clamp(width, 1, MAX_SIZE);
        height = Mth.clamp(height, 1, MAX_SIZE);
        if (dx == controllerDx && dy == controllerDy && dz == controllerDz && width == this.width && height == this.height) {
            return;
        }
        controllerDx = dx;
        controllerDy = dy;
        controllerDz = dz;
        this.width = width;
        this.height = height;
        markUpdated();
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("page", page.id());
        tag.putInt("enabledPages", enabledPages);
        if (dimension != null) {
            tag.putString("dimension", dimension.toString());
        }
        if (!isController()) {
            tag.putIntArray("controller", new int[] {controllerDx, controllerDy, controllerDz});
        }
        tag.putInt("width", width);
        tag.putInt("height", height);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        page = PanelPage.byIdOrSummary(tag.getString("page"));
        int mask = tag.contains("enabledPages") ? tag.getInt("enabledPages") & PanelPage.ALL_MASK : PanelPage.ALL_MASK;
        enabledPages = mask == 0 ? PanelPage.ALL_MASK : mask;
        dimension = tag.contains("dimension") ? ResourceLocation.tryParse(tag.getString("dimension")) : null;
        int[] controller = tag.getIntArray("controller");
        boolean valid = controller.length == 3 && Math.abs(controller[0]) <= MAX_SIZE
                && Math.abs(controller[1]) <= MAX_SIZE && Math.abs(controller[2]) <= MAX_SIZE;
        controllerDx = valid ? controller[0] : 0;
        controllerDy = valid ? controller[1] : 0;
        controllerDz = valid ? controller[2] : 0;
        width = Mth.clamp(tag.getInt("width"), 1, MAX_SIZE);
        height = Mth.clamp(tag.getInt("height"), 1, MAX_SIZE);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
