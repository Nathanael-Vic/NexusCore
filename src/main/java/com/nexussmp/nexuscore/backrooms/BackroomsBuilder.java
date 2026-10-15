package com.nexussmp.nexuscore.backrooms;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Levanta um andar no mundo, um punhado de chunks por tick.
 *
 * <p>Um andar de 700x700 passa de dois milhoes de blocos. Fazer isso de uma vez seguraria o servidor
 * por segundos; entao o trabalho anda em fatias: cada passo pega alguns chunks, escreve as colunas
 * deles e devolve o controle. Os blocos vao sem atualizacao de vizinho (a porta no meio do nada
 * cairia sozinha) e sem recalculo de forma, que e o caro.
 */
public final class BackroomsBuilder {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    /** Sorteios por porta ate juntar os candidatos (os que caem em parede nao contam). */
    private static final int DOOR_TRIES = 4000;
    /** Chaos sorteados para cada porta depois da primeira; fica o mais longe das outras. */
    private static final int DOOR_CANDIDATES = 96;
    /** O minimo que se pede entre a pessoa e qualquer porta, quando o andar permite. */
    private static final int SPAWN_FROM_DOOR = 40;
    /** Sorteio das portas com semente propria: mudar o numero de portas nao muda a planta. */
    private static final long DOOR_SEED_MIX = 0x9E3779B97F4A7C15L;

    private final ServerLevel level;
    private final BackroomsArea area;
    private final BackroomsGrid grid;
    private final boolean ceilingOnly;

    private final int chunkMinX;
    private final int chunkMinZ;
    private final int chunksX;
    private final int chunksZ;
    private int cursor;
    /** Pegada a limpar: o retangulo do andar antigo, quando este esta sendo refeito por cima. */
    @Nullable
    private final BackroomsArea replaced;

    private final BlockState carpet = NexusRegistries.BACKROOMS_CARPET.get().defaultBlockState();
    private final BlockState baseboard = NexusRegistries.BACKROOMS_BASEBOARD.get().defaultBlockState();
    private final BlockState wallpaper = NexusRegistries.BACKROOMS_WALLPAPER.get().defaultBlockState();
    private final BlockState ceiling = NexusRegistries.BACKROOMS_CEILING.get().defaultBlockState();
    private final BlockState lamp = NexusRegistries.BACKROOMS_LAMP.get().defaultBlockState();
    private final BlockState air = Blocks.AIR.defaultBlockState();
    private final BlockPos.MutableBlockPos cursorPos = new BlockPos.MutableBlockPos();

    private BackroomsBuilder(ServerLevel level, BackroomsArea area, BackroomsGrid grid, boolean ceilingOnly,
                             @Nullable BackroomsArea replaced) {
        this.level = level;
        this.area = area;
        this.grid = grid;
        this.ceilingOnly = ceilingOnly;
        this.replaced = replaced;
        // A obra cobre o andar novo e o que ficou do antigo: assim um andar menor nao deixa pedaco
        // do anterior boiando em volta.
        int minX = Math.min(area.originX(), replaced == null ? area.originX() : replaced.originX());
        int minZ = Math.min(area.originZ(), replaced == null ? area.originZ() : replaced.originZ());
        int maxX = Math.max(area.originX() + area.size(), replaced == null ? 0 : replaced.originX() + replaced.size()) - 1;
        int maxZ = Math.max(area.originZ() + area.size(), replaced == null ? 0 : replaced.originZ() + replaced.size()) - 1;
        this.chunkMinX = SectionPos.blockToSectionCoord(minX);
        this.chunkMinZ = SectionPos.blockToSectionCoord(minZ);
        this.chunksX = SectionPos.blockToSectionCoord(maxX) - chunkMinX + 1;
        this.chunksZ = SectionPos.blockToSectionCoord(maxZ) - chunkMinZ + 1;
    }

    /** Andar novo: sorteia a planta, escolhe onde ficam as portas e prepara a construcao. */
    public static BackroomsBuilder create(ServerLevel level, BackroomsArea area, int doorCount,
                                          @Nullable BackroomsArea replaced) {
        BackroomsGrid grid = BackroomsGrid.generate(area.size() / area.cellSize(), area.seed());
        BackroomsBuilder builder = new BackroomsBuilder(level, area, grid, false, replaced);
        area.doors().clear();
        area.doors().addAll(builder.pickDoors(RandomSource.create(area.seed() ^ DOOR_SEED_MIX), doorCount));
        return builder;
    }

    /** So o teto: usado para ligar e desligar a cobertura de um andar que ja existe. */
    public static BackroomsBuilder ceilingOnly(ServerLevel level, BackroomsArea area) {
        BackroomsGrid grid = BackroomsGrid.generate(area.size() / area.cellSize(), area.seed());
        return new BackroomsBuilder(level, area, grid, true, null);
    }

    public int totalChunks() {
        return chunksX * chunksZ;
    }

    public int progressPercent() {
        return totalChunks() == 0 ? 100 : Math.min(100, cursor * 100 / totalChunks());
    }

    /** Constroi mais um naco. {@code true} quando o andar esta pronto. */
    public boolean step(int chunksPerStep) {
        int limit = Math.min(totalChunks(), cursor + chunksPerStep);
        for (; cursor < limit; cursor++) {
            buildChunk(chunkMinX + cursor % chunksX, chunkMinZ + cursor / chunksX);
        }
        if (cursor < totalChunks()) {
            return false;
        }
        if (!ceilingOnly) {
            placeDoors();
        }
        return true;
    }

    private void buildChunk(int chunkX, int chunkZ) {
        // Carrega (ou cria) o chunk uma vez, em vez de uma busca por bloco.
        level.getChunk(chunkX, chunkZ);
        int baseX = SectionPos.sectionToBlockCoord(chunkX);
        int baseZ = SectionPos.sectionToBlockCoord(chunkZ);
        for (int offsetZ = 0; offsetZ < 16; offsetZ++) {
            for (int offsetX = 0; offsetX < 16; offsetX++) {
                int x = baseX + offsetX;
                int z = baseZ + offsetZ;
                if (area.contains(x, z)) {
                    buildColumn(x - area.originX(), z - area.originZ(), x, z);
                } else if (!ceilingOnly && replaced != null && replaced.contains(x, z)) {
                    clearColumn(x, z);
                }
            }
        }
    }

    private void buildColumn(int localX, int localZ, int x, int z) {
        boolean open = grid.isFloor(localX / area.cellSize(), localZ / area.cellSize());

        if (!ceilingOnly) {
            set(x, BackroomsDimensions.FLOOR_Y, z, carpet);
            for (int height = 1; height <= BackroomsDimensions.ROOM_HEIGHT; height++) {
                set(x, BackroomsDimensions.FLOOR_Y + height, z, open ? air : height == 1 ? baseboard : wallpaper);
            }
        }

        if (!area.ceiling()) {
            set(x, BackroomsDimensions.CEILING_Y, z, air);
            return;
        }
        // Painel de luz de tantos em tantos blocos, e so onde ha chao embaixo: lampada dentro de
        // parede seria so custo de luz sem ninguem para ver.
        boolean lit = open && onLampGrid(localX) && onLampGrid(localZ) && lampLit(localX, localZ);
        set(x, BackroomsDimensions.CEILING_Y, z, lit ? lamp : ceiling);
    }

    /** Sobra do andar anterior: some tudo, nao fica parede solta no vazio. */
    private void clearColumn(int x, int z) {
        for (int y = BackroomsDimensions.FLOOR_Y; y <= BackroomsDimensions.CEILING_Y; y++) {
            set(x, y, z, air);
        }
    }

    private boolean onLampGrid(int local) {
        return local % (area.lampSpacing() + 1) == 0;
    }

    /**
     * Nem todo lugar de painel ganha luz.
     *
     * <p>Teto todo aceso fica de supermercado; o que assusta e o corredor que apaga mais adiante. O
     * sorteio sai de uma conta com a semente do andar, entao o mesmo andar tem sempre as mesmas
     * lampadas faltando — inclusive se ele for refeito.
     */
    private boolean lampLit(int localX, int localZ) {
        if (area.lampChance() >= 100) {
            return true;
        }
        long hash = area.seed() ^ (localX * 0x9E3779B97F4A7C15L) ^ (localZ * 0xC2B2AE3D27D4EB4FL);
        hash ^= hash >>> 33;
        hash *= 0xFF51AFD7ED558CCDL;
        hash ^= hash >>> 29;
        return Math.floorMod(hash, 100) < area.lampChance();
    }

    private void set(int x, int y, int z, BlockState state) {
        cursorPos.set(x, y, z);
        level.setBlock(cursorPos, state, SET_FLAGS);
    }

    // ---- portas de saida ----

    /**
     * Onde ficam as portas: em qualquer chao do andar — quarto ou corredor — e o mais longe umas
     * das outras que aquele andar permite.
     *
     * <p>O sorteio e o do "melhor candidato": a primeira porta cai num chao qualquer; para cada uma
     * das seguintes sorteiam-se varios chaos e fica o que esta mais longe de todas as portas ja
     * postas. Continua aleatorio (os candidatos sao sorteados), mas nunca escolhe um ponto perto
     * quando havia um longe — e a distancia se ajusta sozinha ao tamanho do andar, sem numero
     * fixo: num andar grande as portas se espalham por ele inteiro, num pequeno ficam o mais
     * separadas que cabem.
     *
     * <p>Antes a porta pedia chao livre nos oito vizinhos. Num corredor de 3 so a faixa do meio
     * passava nisso, enquanto num quarto quase todo bloco passava — e as portas acabavam quase
     * sempre em quarto. Agora basta ser chao; como ela fica encostada numa parede quando tem uma
     * do lado ({@link #placeDoors}), nao atravessa o caminho de ninguem.
     */
    private List<BlockPos> pickDoors(RandomSource random, int wanted) {
        List<BlockPos> doors = new ArrayList<>();
        if (wanted <= 0) {
            return doors;
        }
        // A distancia entre portas e medida na area, nao no mundo: por isso o sorteio anda em
        // coordenada local e so vira posicao de mundo no fim.
        IntArrayList localX = new IntArrayList();
        IntArrayList localZ = new IntArrayList();
        for (int door = 0; door < wanted; door++) {
            int candidates = localX.isEmpty() ? 1 : DOOR_CANDIDATES;
            int bestX = -1;
            int bestZ = -1;
            long bestDistance = -1;
            int found = 0;
            for (int attempt = 0; attempt < DOOR_TRIES && found < candidates; attempt++) {
                int x = random.nextInt(area.size());
                int z = random.nextInt(area.size());
                if (!isFloor(grid, area.cellSize(), x, z)) {
                    continue;
                }
                found++;
                long distance = nearest(localX, localZ, x, z);
                if (distance > bestDistance) {
                    bestDistance = distance;
                    bestX = x;
                    bestZ = z;
                }
            }
            if (bestX < 0) {
                // Nenhum chao achado: melhor faltar porta do que por uma dentro da parede.
                break;
            }
            localX.add(bestX);
            localZ.add(bestZ);
        }
        for (int i = 0; i < localX.size(); i++) {
            doors.add(new BlockPos(area.originX() + localX.getInt(i),
                    BackroomsDimensions.FLOOR_Y + 1, area.originZ() + localZ.getInt(i)));
        }
        return doors;
    }

    /** Distancia (ao quadrado) ate a porta mais proxima; sem porta nenhuma, e a maior possivel. */
    private static long nearest(IntArrayList xs, IntArrayList zs, int x, int z) {
        long nearest = Long.MAX_VALUE;
        for (int i = 0; i < xs.size(); i++) {
            long dx = xs.getInt(i) - x;
            long dz = zs.getInt(i) - z;
            nearest = Math.min(nearest, dx * dx + dz * dz);
        }
        return nearest;
    }

    /** Distancia entre dois pontos, medida em quadrado (a conta do afastamento do spawn). */
    private static int distance(int x1, int z1, int x2, int z2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(z1 - z2));
    }

    /** Se aquele bloco da area e chao. A planta e em celulas; o bloco cai numa delas. */
    private static boolean isFloor(BackroomsGrid grid, int cellSize, int x, int z) {
        return x >= 0 && z >= 0 && grid.isFloor(x / cellSize, z / cellSize);
    }

    /**
     * Levanta as portas. A direcao sai da semente, como antes, com uma regra a mais: se a porta tem
     * parede do lado, a folha dela vai encostada nessa parede.
     *
     * <p>A folha de uma porta fechada fica na beirada <i>oposta</i> a direcao para onde ela olha
     * (virada para leste, a folha esta na beira oeste do bloco). Encostada na parede ela parece
     * posta ali de proposito, e nunca fica atravessada num corredor — nem no de um bloco so.
     * Sem parede em volta, no meio de um quarto, ela continua solta e virada para qualquer lado.
     */
    private void placeDoors() {
        RandomSource random = RandomSource.create(area.seed());
        List<Direction> walls = new ArrayList<>(4);
        for (BlockPos door : area.doors()) {
            int x = door.getX() - area.originX();
            int z = door.getZ() - area.originZ();
            walls.clear();
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (!isFloor(grid, area.cellSize(), x + side.getStepX(), z + side.getStepZ())) {
                    walls.add(side);
                }
            }
            Direction facing = walls.isEmpty()
                    ? Direction.from2DDataValue(random.nextInt(4))
                    : walls.get(random.nextInt(walls.size())).getOpposite();
            BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
                    .setValue(DoorBlock.FACING, facing)
                    .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                    .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            level.setBlock(door, lower, SET_FLAGS);
            level.setBlock(door.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), SET_FLAGS);
        }
    }

    /**
     * Onde a pessoa cai: um chao qualquer do andar — quarto ou corredor —, <b>longe das portas</b>.
     *
     * <p>Refaz a planta a partir da semente em vez de varrer o mundo: sai na mesma e nao depende de
     * os chunks daquele canto estarem carregados.
     *
     * <p>Cair perto de uma saida estragaria o lugar: em dez segundos a pessoa acha a porta e volta,
     * e nada do que foi construido aconteceu. Entao primeiro se procura um ponto bem longe de
     * qualquer porta ({@link #spawnFromDoor}, que cresce com o andar); se o andar nao tiver lugar
     * assim, a exigencia cai pela metade, e depois some.
     */
    public static BlockPos findSpawn(BackroomsArea area, RandomSource random) {
        BackroomsGrid grid = BackroomsGrid.generate(area.size() / area.cellSize(), area.seed());
        int margin = Math.min(area.cellSize() * 2, area.size() / 8);
        for (int far = spawnFromDoor(area); far >= 0; far /= 2) {
            for (int attempt = 0; attempt < 600; attempt++) {
                int x = Mth.nextInt(random, margin, Math.max(margin + 1, area.size() - margin));
                int z = Mth.nextInt(random, margin, Math.max(margin + 1, area.size() - margin));
                if (isFloor(grid, area.cellSize(), x, z) && awayFromDoors(area, x, z, far)) {
                    return new BlockPos(area.originX() + x, BackroomsDimensions.FLOOR_Y + 1,
                            area.originZ() + z);
                }
            }
            if (far == 0) {
                break;
            }
        }
        return new BlockPos(area.centerX(), BackroomsDimensions.FLOOR_Y + 1, area.centerZ());
    }

    /**
     * O quao longe das portas a pessoa cai. Nunca menos que {@link #SPAWN_FROM_DOOR}; num andar
     * grande, um quinto do lado dele — num de 700, 140 blocos ate a saida mais proxima.
     */
    private static int spawnFromDoor(BackroomsArea area) {
        return Math.max(SPAWN_FROM_DOOR, area.size() / 5);
    }

    /** As portas guardadas sao posicoes de mundo; o sorteio anda em coordenada de area. */
    private static boolean awayFromDoors(BackroomsArea area, int x, int z, int minimum) {
        if (minimum <= 0) {
            return true;
        }
        for (BlockPos door : area.doors()) {
            if (distance(door.getX() - area.originX(), door.getZ() - area.originZ(), x, z) < minimum) {
                return false;
            }
        }
        return true;
    }

    public BackroomsArea area() {
        return area;
    }
}
