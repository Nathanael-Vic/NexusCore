package com.nexussmp.nexuscore.backrooms;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Um andar ja gerado: onde ele fica, com que semente nasceu e onde ficaram as portas de saida.
 *
 * <p>Guardar a semente e o tamanho e o que permite refazer o andar identico (ou so o teto dele) sem
 * varrer o mundo bloco a bloco.
 */
public final class BackroomsArea {
    private final int originX;
    private final int originZ;
    private final int size;
    private final int cellSize;
    private final long seed;
    private final int lampSpacing;
    private final int lampChance;
    private boolean ceiling;
    private final List<BlockPos> doors = new ArrayList<>();

    public BackroomsArea(int originX, int originZ, int size, int cellSize, long seed, boolean ceiling,
                         int lampSpacing, int lampChance) {
        this.originX = originX;
        this.originZ = originZ;
        this.size = size;
        this.cellSize = cellSize;
        this.seed = seed;
        this.ceiling = ceiling;
        this.lampSpacing = lampSpacing;
        this.lampChance = lampChance;
    }

    public int originX() {
        return originX;
    }

    public int originZ() {
        return originZ;
    }

    public int size() {
        return size;
    }

    public int cellSize() {
        return cellSize;
    }

    public long seed() {
        return seed;
    }

    /** Blocos de forro entre uma luz e a seguinte. */
    public int lampSpacing() {
        return lampSpacing;
    }

    /** Chance, em porcento, de um lugar de painel ganhar luz de verdade. */
    public int lampChance() {
        return lampChance;
    }

    public boolean ceiling() {
        return ceiling;
    }

    public void setCeiling(boolean ceiling) {
        this.ceiling = ceiling;
    }

    public List<BlockPos> doors() {
        return doors;
    }

    public boolean contains(int x, int z) {
        return x >= originX && z >= originZ && x < originX + size && z < originZ + size;
    }

    public int centerX() {
        return originX + size / 2;
    }

    public int centerZ() {
        return originZ + size / 2;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("originX", originX);
        tag.putInt("originZ", originZ);
        tag.putInt("size", size);
        tag.putInt("cellSize", cellSize);
        tag.putLong("seed", seed);
        tag.putBoolean("ceiling", ceiling);
        tag.putInt("lampSpacing", lampSpacing);
        tag.putInt("lampChance", lampChance);
        ListTag list = new ListTag();
        for (BlockPos door : doors) {
            list.add(net.minecraft.nbt.LongTag.valueOf(door.asLong()));
        }
        tag.put("doors", list);
        return tag;
    }

    public static BackroomsArea load(CompoundTag tag) {
        BackroomsArea area = new BackroomsArea(
                tag.getInt("originX"), tag.getInt("originZ"), tag.getInt("size"),
                Math.max(1, tag.getInt("cellSize")), tag.getLong("seed"),
                tag.getBoolean("ceiling"), Math.max(1, tag.getInt("lampSpacing")),
                tag.contains("lampChance") ? tag.getInt("lampChance") : 100);
        for (Tag door : tag.getList("doors", Tag.TAG_LONG)) {
            area.doors.add(BlockPos.of(((net.minecraft.nbt.LongTag) door).getAsLong()));
        }
        return area;
    }
}
