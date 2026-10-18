package com.nexussmp.nexuscore.monolith;

import com.mojang.serialization.Codec;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * O estrago em volta dos monolitos, feito junto com o mundo: roda uma vez em cada chunk, no fim da
 * decoracao, e segue o plano que o {@link Echo} da para ele. O que fica e terreno de verdade, salvo
 * no mundo, igual para todo mundo.
 *
 * <p>So mexe no proprio chunk, e nunca num chunk por onde passa um monolito: a estrutura fica inteira.
 */
public final class EchoFeature extends Feature<NoneFeatureConfiguration> {
    private static final Block[] WRONG_LEAVES = {Blocks.CHERRY_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES,
            Blocks.NETHER_WART_BLOCK, Blocks.WARPED_WART_BLOCK};
    private static final Block[] ORES = {Blocks.COAL_ORE, Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE,
            Blocks.GOLD_ORE, Blocks.REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_DIAMOND_ORE};
    private static final Block[] BADLANDS_BANDS = {Blocks.ORANGE_TERRACOTTA, Blocks.TERRACOTTA,
            Blocks.YELLOW_TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.TERRACOTTA, Blocks.RED_TERRACOTTA};

    /** As placas: quatro linhas cada; {@code %x}/{@code %z} viram as coordenadas do chunk. */
    private static final String[][] SIGNS = {
            {"ERR:", "chunk %x,%z", "não", "encontrado"},
            {"NullPointer", "Exception", "at monolith", ".<init>"},
            {"entity_0482", "não", "responde", ""},
            {"missing:", "texture", "minecraft:", "???"},
            {"seed", "corrompida", "", "%x %z"},
            {"[ ]", "aguardando", "carregamento", "..."},
            {"█████", "██ ████ █", "███", "██████"},
            {"você não", "devia estar", "aqui", ""},
            {"at tick()", "at tick()", "at tick()", "at ???"},
            {"level.dat", "0 bytes", "", ""},
    };

    public EchoFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ServerLevel server = level.getLevel();
        if (server.dimension() != Level.OVERWORLD) {
            return false;
        }
        List<MonolithSites.Site> sites = MonolithSites.of(server);
        if (sites.isEmpty()) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(context.origin());
        ChunkAccess access = level.getChunk(chunk.x, chunk.z);
        if (touchesMonolith(server, access)) {
            return false;
        }
        Echo.Plan plan = Echo.plan(server.getSeed(), chunk, sites);
        if (plan.empty()) {
            return false;
        }
        RandomSource random = new XoroshiroRandomSource(plan.seed() ^ 0x5851F42D4C957F2DL);
        if (plan.hollow()) {
            hollow(access);
            return true;
        }
        if (plan.skin() != null) {
            reskin(level, server, access, chunk, plan.skin(), plan.shift(), random);
        }
        if (plan.wrongLeaves()) {
            wrongLeaves(level, chunk, random);
        }
        if (plan.ore()) {
            ore(level, chunk, random);
        }
        for (int i = 0; i < plan.floating(); i++) {
            floating(level, chunk, random);
        }
        if (plan.invertedTree()) {
            invertedTree(level, chunk, random);
        }
        if (plan.sign()) {
            sign(level, chunk, random);
        }
        if (plan.nullPatch()) {
            nullPatch(level, chunk, random);
        }
        return true;
    }

    private static boolean touchesMonolith(ServerLevel server, ChunkAccess access) {
        for (Structure structure : access.getAllReferences().keySet()) {
            if (MonolithSites.isMonolithStructure(server, structure)) {
                return true;
            }
        }
        for (Structure structure : access.getAllStarts().keySet()) {
            if (MonolithSites.isMonolithStructure(server, structure)) {
                return true;
            }
        }
        return false;
    }

    // ---- o vazio ----

    /** O chunk inteiro some, da superficie ao fundo: so o buraco quadrado e o vazio la embaixo. */
    private static void hollow(ChunkAccess access) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (LevelChunkSection section : access.getSections()) {
            if (section.hasOnlyAir()) {
                continue;
            }
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        section.setBlockState(x, y, z, air, false);
                    }
                }
            }
        }
        for (BlockPos pos : new ArrayList<>(access.getBlockEntitiesPos())) {
            access.removeBlockEntity(pos);
        }
        Heightmap.primeHeightmaps(access, EnumSet.allOf(Heightmap.Types.class));
        access.setUnsaved(true);
    }

    // ---- o bioma errado ----

    private static void reskin(WorldGenLevel level, ServerLevel server, ChunkAccess access, ChunkPos chunk,
                               Echo.Skin skin, int shift, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunk.getMinBlockX() + dx;
                int z = chunk.getMinBlockZ() + dz;
                int columnTop = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                int surface = surface(level, pos, x, z);
                if (surface <= level.getMinBuildHeight()) {
                    continue;
                }
                // Arvore e planta do bioma de antes saem; o que passava da borda fica cortado reto.
                for (int y = surface + 1; y <= columnTop; y++) {
                    level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
                BlockState top = level.getBlockState(pos.set(x, surface, z));
                if (!top.getFluidState().isEmpty()) {
                    if (skin == Echo.Skin.SNOW && top.is(Blocks.WATER)) {
                        level.setBlock(pos, Blocks.ICE.defaultBlockState(), 2);
                    }
                    continue;
                }
                int ground = surface + shift;
                if (shift > 0) {
                    for (int y = surface + 1; y <= ground; y++) {
                        level.setBlock(pos.set(x, y, z), under(skin, 1), 2);
                    }
                } else if (shift < 0) {
                    for (int y = ground + 1; y <= surface; y++) {
                        level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                paint(level, pos, x, ground, z, skin);
                decorate(level, pos, x, ground + 1, z, skin, random);
            }
        }
        Holder<Biome> biome = server.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biomeOf(skin));
        access.fillBiomesFromNoise((qx, qy, qz, sampler) -> biome, server.getChunkSource().randomState().sampler());
        access.setUnsaved(true);
    }

    private static ResourceKey<Biome> biomeOf(Echo.Skin skin) {
        return switch (skin) {
            case SNOW -> Biomes.SNOWY_PLAINS;
            case DESERT -> Biomes.DESERT;
            case BADLANDS -> Biomes.BADLANDS;
            case MUSHROOM -> Biomes.MUSHROOM_FIELDS;
            case CHERRY -> Biomes.CHERRY_GROVE;
        };
    }

    /** O chao do bioma novo: o bloco de cima e uns poucos abaixo, sem tapar caverna. */
    private static void paint(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int ground, int z, Echo.Skin skin) {
        int depth = skin == Echo.Skin.DESERT || skin == Echo.Skin.BADLANDS ? 7 : 4;
        for (int i = 0; i < depth; i++) {
            pos.set(x, ground - i, z);
            if (level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) {
                break;
            }
            level.setBlock(pos, i == 0 ? top(skin) : under(skin, i), 2);
        }
    }

    private static BlockState top(Echo.Skin skin) {
        return switch (skin) {
            case SNOW -> Blocks.GRASS_BLOCK.defaultBlockState().setValue(SnowyDirtBlock.SNOWY, true);
            case DESERT -> Blocks.SAND.defaultBlockState();
            case BADLANDS -> Blocks.RED_SAND.defaultBlockState();
            case MUSHROOM -> Blocks.MYCELIUM.defaultBlockState();
            case CHERRY -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

    private static BlockState under(Echo.Skin skin, int depth) {
        return switch (skin) {
            case DESERT -> depth < 4 ? Blocks.SAND.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState();
            case BADLANDS -> depth < 2 ? Blocks.RED_SAND.defaultBlockState() : BADLANDS_BANDS[depth % BADLANDS_BANDS.length].defaultBlockState();
            default -> Blocks.DIRT.defaultBlockState();
        };
    }

    private static void decorate(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z, Echo.Skin skin,
                                 RandomSource random) {
        pos.set(x, y, z);
        if (!level.getBlockState(pos).isAir()) {
            return;
        }
        switch (skin) {
            case SNOW -> level.setBlock(pos, Blocks.SNOW.defaultBlockState(), 2);
            case DESERT -> {
                float roll = random.nextFloat();
                if (roll < 0.012F) {
                    int height = 1 + random.nextInt(3);
                    for (int i = 0; i < height; i++) {
                        level.setBlock(pos.set(x, y + i, z), Blocks.CACTUS.defaultBlockState(), 2);
                    }
                } else if (roll < 0.035F) {
                    level.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), 2);
                }
            }
            case BADLANDS -> {
                if (random.nextFloat() < 0.02F) {
                    level.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), 2);
                }
            }
            case MUSHROOM -> {
                if (random.nextFloat() < 0.025F) {
                    level.setBlock(pos, (random.nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState(), 2);
                }
            }
            case CHERRY -> {
                if (random.nextFloat() < 0.3F) {
                    level.setBlock(pos, Blocks.PINK_PETALS.defaultBlockState()
                            .setValue(PinkPetalsBlock.AMOUNT, 1 + random.nextInt(4))
                            .setValue(PinkPetalsBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)), 2);
                }
            }
        }
    }

    // ---- as coisas pequenas ----

    /** Uma copa de arvore com a folha da cor errada. */
    private static void wrongLeaves(WorldGenLevel level, ChunkPos chunk, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Block replacement = WRONG_LEAVES[random.nextInt(WRONG_LEAVES.length)];
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = chunk.getMinBlockX() + 3 + random.nextInt(10);
            int z = chunk.getMinBlockZ() + 3 + random.nextInt(10);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
            if (!level.getBlockState(pos.set(x, y, z)).is(BlockTags.LEAVES)) {
                continue;
            }
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -4; dy <= 1; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        BlockPos leaf = new BlockPos(x + dx, y + dy, z + dz);
                        if (inside(chunk, leaf) && level.getBlockState(leaf).is(BlockTags.LEAVES)) {
                            level.setBlock(leaf, persistent(replacement.defaultBlockState()), 2);
                        }
                    }
                }
            }
            return;
        }
    }

    /** Um minerio no lugar de um bloco de grama (ou de areia). */
    private static void ore(WorldGenLevel level, ChunkPos chunk, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = chunk.getMinBlockX() + random.nextInt(16);
            int z = chunk.getMinBlockZ() + random.nextInt(16);
            int y = surface(level, pos, x, z);
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)) {
                level.setBlock(pos, ORES[random.nextInt(ORES.length)].defaultBlockState(), 2);
                return;
            }
        }
    }

    /** Uns blocos do proprio chao, soltos no ar. */
    private static void floating(WorldGenLevel level, ChunkPos chunk, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = chunk.getMinBlockX() + 2 + random.nextInt(12);
        int z = chunk.getMinBlockZ() + 2 + random.nextInt(12);
        int y = surface(level, pos, x, z);
        BlockState ground = level.getBlockState(pos.set(x, y, z));
        if (!ground.getFluidState().isEmpty() || y <= level.getMinBuildHeight()) {
            return;
        }
        BlockState block = isClutter(ground) ? Blocks.DIRT.defaultBlockState() : ground;
        int height = y + 4 + random.nextInt(9);
        int size = 1 + random.nextInt(3);
        BlockPos at = new BlockPos(x, height, z);
        for (int i = 0; i < size; i++) {
            if (inside(chunk, at) && level.getBlockState(at).isAir()) {
                level.setBlock(at, block, 2);
            }
            at = at.relative(Direction.getRandom(random));
        }
    }

    /** Uma arvore de cabeca para baixo: a copa no chao e o tronco subindo dela. */
    private static void invertedTree(WorldGenLevel level, ChunkPos chunk, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = chunk.getMinBlockX() + 3 + random.nextInt(10);
        int z = chunk.getMinBlockZ() + 3 + random.nextInt(10);
        int y = surface(level, pos, x, z);
        if (!level.getBlockState(pos.set(x, y, z)).getFluidState().isEmpty() || y <= level.getMinBuildHeight()) {
            return;
        }
        BlockState leaves = persistent(Blocks.OAK_LEAVES.defaultBlockState());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 1; dy <= 3; dy++) {
                    if (dx * dx + dz * dz + (dy - 2) * (dy - 2) <= 5) {
                        setIfFree(level, chunk, new BlockPos(x + dx, y + dy, z + dz), leaves);
                    }
                }
            }
        }
        int trunk = 4 + random.nextInt(3);
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        for (int dy = 1; dy <= trunk; dy++) {
            level.setBlock(pos.set(x, y + 3 + dy, z), log, 2);
        }
        // Os "galhos" no alto, deitados, como raiz exposta.
        int crown = y + 3 + trunk;
        setIfFree(level, chunk, new BlockPos(x + 1, crown, z), log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        setIfFree(level, chunk, new BlockPos(x - 1, crown, z), log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        setIfFree(level, chunk, new BlockPos(x, crown, z + 1), log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
    }

    /** Uma placa com texto de erro, brilhando em roxo. Encerada: ninguem reescreve. */
    private static void sign(WorldGenLevel level, ChunkPos chunk, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = chunk.getMinBlockX() + 2 + random.nextInt(12);
        int z = chunk.getMinBlockZ() + 2 + random.nextInt(12);
        int y = surface(level, pos, x, z);
        if (!level.getBlockState(pos.set(x, y, z)).getFluidState().isEmpty() || y <= level.getMinBuildHeight()) {
            return;
        }
        BlockPos at = new BlockPos(x, y + 1, z);
        if (!level.getBlockState(at).canBeReplaced()) {
            return;
        }
        level.setBlock(at, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, random.nextInt(16)), 2);
        BlockEntity entity = level.getBlockEntity(at);
        if (!(entity instanceof SignBlockEntity sign)) {
            return;
        }
        String[] lines = SIGNS[random.nextInt(SIGNS.length)];
        SignText text = new SignText().setHasGlowingText(true).setColor(DyeColor.PURPLE);
        for (int i = 0; i < 4; i++) {
            text = text.setMessage(i, Component.literal(lines[i]
                    .replace("%x", Integer.toString(chunk.x)).replace("%z", Integer.toString(chunk.z))));
        }
        // O BlockEntity ainda nao tem mundo nesta fase: o texto entra como se viesse do disco.
        CompoundTag tag = new CompoundTag();
        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        SignText.DIRECT_CODEC.encodeStart(ops, text).result().ifPresent(encoded -> tag.put("front_text", encoded));
        SignText.DIRECT_CODEC.encodeStart(ops, text).result().ifPresent(encoded -> tag.put("back_text", encoded));
        tag.putBoolean("is_waxed", true);
        sign.loadWithComponents(tag, level.registryAccess());
    }

    /** Uma mancha de blocos sem textura no chao, as vezes subindo numa colunazinha. */
    private static void nullPatch(WorldGenLevel level, ChunkPos chunk, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState nothing = NexusRegistries.NULL_BLOCK.get().defaultBlockState();
        int blobs = 1 + random.nextInt(2);
        for (int b = 0; b < blobs; b++) {
            int cx = chunk.getMinBlockX() + 2 + random.nextInt(12);
            int cz = chunk.getMinBlockZ() + 2 + random.nextInt(12);
            int radius = 1 + random.nextInt(2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius || random.nextFloat() < 0.25F) {
                        continue;
                    }
                    int x = cx + dx;
                    int z = cz + dz;
                    int y = surface(level, pos, x, z);
                    if (y > level.getMinBuildHeight() && level.getBlockState(pos.set(x, y, z)).getFluidState().isEmpty()) {
                        level.setBlock(pos, nothing, 2);
                    }
                }
            }
            if (random.nextFloat() < 0.3F) {
                int y = surface(level, pos, cx, cz);
                int height = 1 + random.nextInt(3);
                for (int i = 1; i <= height; i++) {
                    setIfFree(level, chunk, new BlockPos(cx, y + i, cz), nothing);
                }
            }
        }
    }

    // ---- ajudantes ----

    /**
     * A altura do chao de verdade numa coluna: desce pulando arvore, folha, planta e neve fina. Para
     * na agua (devolve a altura da superficie dela).
     */
    private static int surface(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z) {
        pos.set(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1, z);
        while (pos.getY() > level.getMinBuildHeight()) {
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || (!state.isAir() && !isClutter(state))) {
                return pos.getY();
            }
            pos.move(Direction.DOWN);
        }
        return level.getMinBuildHeight();
    }

    private static boolean isClutter(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.getBlock() instanceof BushBlock || state.is(Blocks.CACTUS) || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.BAMBOO) || state.is(Blocks.SNOW) || state.is(Blocks.COCOA) || state.is(Blocks.BEE_NEST)
                || state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(Blocks.VINE) || state.is(Blocks.MOSS_CARPET) || state.is(Blocks.PINK_PETALS);
    }

    private static BlockState persistent(BlockState state) {
        return state.getBlock() instanceof LeavesBlock ? state.setValue(LeavesBlock.PERSISTENT, true) : state;
    }

    private static void setIfFree(WorldGenLevel level, ChunkPos chunk, BlockPos pos, BlockState state) {
        if (inside(chunk, pos) && level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, state, 2);
        }
    }

    private static boolean inside(ChunkPos chunk, BlockPos pos) {
        return pos.getX() >= chunk.getMinBlockX() && pos.getX() <= chunk.getMaxBlockX()
                && pos.getZ() >= chunk.getMinBlockZ() && pos.getZ() <= chunk.getMaxBlockZ();
    }
}
