package com.nexussmp.nexuscore.core.registry;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.backrooms.block.BackroomsLampBlock;
import com.nexussmp.nexuscore.backrooms.entity.SkinwalkerEntity;
import com.nexussmp.nexuscore.display.block.PanelBlock;
import com.nexussmp.nexuscore.display.blockentity.PanelBlockEntity;
import com.nexussmp.nexuscore.hologram.entity.HologramEntity;
import com.nexussmp.nexuscore.morph.MorphForm;
import com.nexussmp.nexuscore.morph.MorphRingItem;
import com.nexussmp.nexuscore.parrot.ShoulderParrot;
import com.nexussmp.nexuscore.quest.block.QuestBoardBlock;
import com.nexussmp.nexuscore.quest.block.QuestBoardPartBlock;
import com.nexussmp.nexuscore.relic.RelicItem;
import com.nexussmp.nexuscore.relic.astral.AstralBody;
import com.nexussmp.nexuscore.relic.astral.AstralMaskItem;
import com.nexussmp.nexuscore.relic.bag.BottomlessBagItem;
import com.nexussmp.nexuscore.relic.flight.WindFeatherItem;
import com.nexussmp.nexuscore.relic.heart.EmberHeartItem;
import com.nexussmp.nexuscore.relic.purity.PurityAmuletItem;
import com.nexussmp.nexuscore.relic.refuge.RefugeDoorBlock;
import com.nexussmp.nexuscore.relic.refuge.RefugeDoorBlockEntity;
import com.nexussmp.nexuscore.relic.refuge.RefugeKeyItem;
import com.nexussmp.nexuscore.relic.stone.UnyieldingStoneBlock;
import com.nexussmp.nexuscore.relic.stone.UnyieldingStoneBlockEntity;
import com.nexussmp.nexuscore.relic.stone.UnyieldingStoneItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Todos os registros do jogo (blocos, itens, block entities, aba do criativo).
 *
 * <p>Registrados sempre, sem depender de modulo ligado: sumir com um bloco quebraria mundos
 * e a conexao cliente-servidor. O liga/desliga dos modulos so controla comportamento.
 */
public final class NexusRegistries {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NexusCore.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NexusCore.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NexusCore.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NexusCore.MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NexusCore.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, NexusCore.MOD_ID);

    public static final DeferredBlock<PanelBlock> EXPLORATION_PANEL = BLOCKS.registerBlock("exploration_panel",
            PanelBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    // Pistao nao arrasta um pedaco da tela para fora do lugar.
                    .pushReaction(PushReaction.BLOCK));

    public static final DeferredItem<BlockItem> EXPLORATION_PANEL_ITEM = ITEMS.registerSimpleBlockItem(EXPLORATION_PANEL);

    /** Mural de quests: o bloco onde as pessoas assumem e publicam trabalhos. */
    public static final DeferredBlock<QuestBoardBlock> QUEST_BOARD = BLOCKS.registerBlock("quest_board",
            QuestBoardBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());

    public static final DeferredItem<BlockItem> QUEST_BOARD_ITEM = ITEMS.registerSimpleBlockItem(QUEST_BOARD);

    /**
     * Os cinco pedacos invisiveis do mural.
     *
     * <p>Sem item proprio: nao se coloca um pedaco, ele nasce junto com o mural. Sem tabela de drop
     * tambem — quebrar qualquer parte quebra o mural, e o item sai do bloco principal.
     */
    public static final DeferredBlock<QuestBoardPartBlock> QUEST_BOARD_PART = BLOCKS.registerBlock("quest_board_part",
            QuestBoardPartBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noLootTable()
                    .ignitedByLava());

    // ---- backrooms ----
    // Blocos sem segredo: cubo de textura unica. O que faz o lugar e o gerador, nao o bloco.
    // Indestrutiveis como a bedrock (explosao, pistao e Wither inclusive), sem drop e sem receita:
    // so o gerador e o criativo poem e tiram. Nao ha como cavar saida nem levar um pedaco embora.

    /** Papel de parede amarelo: as paredes de cima. */
    public static final DeferredBlock<Block> BACKROOMS_WALLPAPER = BLOCKS.registerSimpleBlock("backrooms_wallpaper",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.WOOD));

    /** Rodape: a fiada de baixo das paredes. */
    public static final DeferredBlock<Block> BACKROOMS_BASEBOARD = BLOCKS.registerSimpleBlock("backrooms_baseboard",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<Block> BACKROOMS_CARPET = BLOCKS.registerSimpleBlock("backrooms_carpet",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_YELLOW)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.WOOL));

    public static final DeferredBlock<Block> BACKROOMS_CEILING = BLOCKS.registerSimpleBlock("backrooms_ceiling",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.STONE));

    /** Painel de luz do teto: e ele que ilumina o andar inteiro — e que apaga na hora do susto. */
    public static final DeferredBlock<BackroomsLampBlock> BACKROOMS_LAMP = BLOCKS.registerBlock("backrooms_lamp",
            BackroomsLampBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> state.getValue(BackroomsLampBlock.LIT) ? 15 : 0));

    public static final DeferredItem<BlockItem> BACKROOMS_WALLPAPER_ITEM = ITEMS.registerSimpleBlockItem(BACKROOMS_WALLPAPER);
    public static final DeferredItem<BlockItem> BACKROOMS_BASEBOARD_ITEM = ITEMS.registerSimpleBlockItem(BACKROOMS_BASEBOARD);
    public static final DeferredItem<BlockItem> BACKROOMS_CARPET_ITEM = ITEMS.registerSimpleBlockItem(BACKROOMS_CARPET);
    public static final DeferredItem<BlockItem> BACKROOMS_CEILING_ITEM = ITEMS.registerSimpleBlockItem(BACKROOMS_CEILING);
    public static final DeferredItem<BlockItem> BACKROOMS_LAMP_ITEM = ITEMS.registerSimpleBlockItem(BACKROOMS_LAMP);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PanelBlockEntity>> EXPLORATION_PANEL_ENTITY =
            BLOCK_ENTITIES.register("exploration_panel",
                    () -> BlockEntityType.Builder.of(PanelBlockEntity::new, EXPLORATION_PANEL.get()).build(null));

    /**
     * O skinwalker.
     *
     * <p>Sincronizado a cada tick como o holograma: ele imita gente, e movimento de gente picotado
     * denuncia na hora que nao e.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<SkinwalkerEntity>> SKINWALKER =
            ENTITY_TYPES.register("skinwalker",
                    () -> EntityType.Builder.<SkinwalkerEntity>of(SkinwalkerEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("skinwalker"));

    /** Aba "NexusCore" no criativo: todo item registrado pelo mod entra nela automaticamente. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nexuscore"))
                    .icon(() -> new ItemStack(EXPLORATION_PANEL_ITEM.get()))
                    .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    /**
     * O holograma: uma entidade sem colisao, sincronizada a cada tick para a reproducao sair fiel
     * ao movimento gravado (o padrao do jogo, a cada 3 ticks, engoliria gestos curtos).
     */
    public static final DeferredHolder<EntityType<?>, EntityType<HologramEntity>> HOLOGRAM =
            ENTITY_TYPES.register("hologram", () -> EntityType.Builder.<HologramEntity>of(HologramEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("hologram"));

    /**
     * O papagaio de ombro. Nunca salvo (renasce no ombro do dono) e sem ovo: so o comando poe um.
     * Hitbox menor que a de um papagaio solto: ele fica sentado, e a caixa e o que o Jade mira.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<ShoulderParrot>> SHOULDER_PARROT =
            ENTITY_TYPES.register("shoulder_parrot", () -> EntityType.Builder.<ShoulderParrot>of(ShoulderParrot::new, MobCategory.MISC)
                    .sized(0.4F, 0.55F)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build("shoulder_parrot"));

    /** A forma guardada no anel da metamorfose. Vai para o disco e para a rede junto com o item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MorphForm>> MORPH_FORM =
            DATA_COMPONENTS.registerComponentType("morph_form",
                    builder -> builder.persistent(MorphForm.CODEC).networkSynchronized(MorphForm.STREAM_CODEC));

    /** O anel da metamorfose. Sem receita: quem entrega e um operador. */
    public static final DeferredItem<MorphRingItem> MORPH_RING = ITEMS.registerItem("morph_ring",
            MorphRingItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    // ---- reliquias ----
    // Todas ficam com quem as carrega na morte (RelicKeeper), nao queimam, nao somem no chao, e nao
    // empilham. Sem receita: quem entrega e um operador, como o anel.

    /**
     * Quando a reliquia volta a funcionar, em tempo de jogo: recarga da chave e da mascara, cinzas do
     * coracao, proxima gota do amuleto. Vai no item — trocar de mao, relogar ou morrer nao zera.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RELIC_READY_AT =
            DATA_COMPONENTS.registerComponentType("relic_ready_at",
                    builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** As gotas que sobram no amuleto da pureza. Sem o componente, ele esta cheio. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PURITY_DROPS =
            DATA_COMPONENTS.registerComponentType("purity_drops",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredItem<RefugeKeyItem> REFUGE_KEY = ITEMS.registerItem("refuge_key",
            RefugeKeyItem::new, RelicItem.properties());
    public static final DeferredItem<WindFeatherItem> WIND_FEATHER = ITEMS.registerItem("wind_feather",
            WindFeatherItem::new, RelicItem.properties());
    public static final DeferredItem<EmberHeartItem> EMBER_HEART = ITEMS.registerItem("ember_heart",
            EmberHeartItem::new, RelicItem.properties());
    public static final DeferredItem<AstralMaskItem> ASTRAL_MASK = ITEMS.registerItem("astral_mask",
            AstralMaskItem::new, RelicItem.properties());
    public static final DeferredItem<PurityAmuletItem> PURITY_AMULET = ITEMS.registerItem("purity_amulet",
            PurityAmuletItem::new, RelicItem.properties());
    public static final DeferredItem<BottomlessBagItem> BOTTOMLESS_BAG = ITEMS.registerItem("bottomless_bag",
            BottomlessBagItem::new, RelicItem.properties());

    /**
     * As paredes do refugio: branco liso que nao quebra, nao explode e nao se move. E elas que
     * iluminam a sala — luz cheia, para nenhum bicho nascer la dentro.
     */
    public static final DeferredBlock<Block> REFUGE_WALL = BLOCKS.registerSimpleBlock("refuge_wall",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 15));

    /** A porta do refugio: a que a chave abre por alguns segundos, e a de saida, la dentro. */
    public static final DeferredBlock<RefugeDoorBlock> REFUGE_DOOR = BLOCKS.registerBlock("refuge_door",
            RefugeDoorBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 7));

    /** A pedra inabalavel: so quem a colocou tira. Nem explosao, nem pistao, nem Wither. */
    public static final DeferredBlock<UnyieldingStoneBlock> UNYIELDING_STONE = BLOCKS.registerBlock("unyielding_stone",
            UnyieldingStoneBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.DEEPSLATE_BRICKS));

    public static final DeferredItem<UnyieldingStoneItem> UNYIELDING_STONE_ITEM = ITEMS.registerItem("unyielding_stone",
            properties -> new UnyieldingStoneItem(UNYIELDING_STONE.get(), properties), RelicItem.properties());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RefugeDoorBlockEntity>> REFUGE_DOOR_ENTITY =
            BLOCK_ENTITIES.register("refuge_door",
                    () -> BlockEntityType.Builder.of(RefugeDoorBlockEntity::new, REFUGE_DOOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UnyieldingStoneBlockEntity>> UNYIELDING_STONE_ENTITY =
            BLOCK_ENTITIES.register("unyielding_stone",
                    () -> BlockEntityType.Builder.of(UnyieldingStoneBlockEntity::new, UNYIELDING_STONE.get()).build(null));

    /**
     * O corpo que fica para tras quando alguem sai dele com a mascara astral. Nunca salvo: se o
     * servidor cair, quem volta e a pessoa, para onde o corpo estava (AstralSessions).
     */
    public static final DeferredHolder<EntityType<?>, EntityType<AstralBody>> ASTRAL_BODY =
            ENTITY_TYPES.register("astral_body", () -> EntityType.Builder.<AstralBody>of(AstralBody::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("astral_body"));

    public static void register(IEventBus modBus) {
        DATA_COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        CREATIVE_TABS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener(EntityAttributeCreationEvent.class, event -> {
            // Toda entidade viva precisa estar aqui: o construtor de LivingEntity ja le a vida
            // maxima, e sem tabela de atributos isso estoura antes de a entidade existir.
            event.put(HOLOGRAM.get(), HologramEntity.createAttributes().build());
            event.put(SKINWALKER.get(), SkinwalkerEntity.createAttributes().build());
            event.put(SHOULDER_PARROT.get(), ShoulderParrot.createAttributes().build());
            event.put(ASTRAL_BODY.get(), AstralBody.createAttributes().build());
        });
    }

    private NexusRegistries() {}
}
