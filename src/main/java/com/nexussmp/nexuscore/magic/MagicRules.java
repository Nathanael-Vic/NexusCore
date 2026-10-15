package com.nexussmp.nexuscore.magic;

import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.ArchevokerLogbookItem;
import io.redspace.ironsspellbooks.item.ChronicleItem;
import io.redspace.ironsspellbooks.item.EldritchManuscript;
import io.redspace.ironsspellbooks.item.FurledMapItem;
import io.redspace.ironsspellbooks.item.ReadableLoreItem;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.item.WaywardCompass;
import io.redspace.ironsspellbooks.item.consumables.DrinkableItem;
import io.redspace.ironsspellbooks.item.weapons.AutoloaderCrossbow;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Set;

/**
 * Tudo o que o Iron's Spells deixa fazer, fechado para quem nao e mago.
 *
 * <p>A magia em si — lancar pelo livro, pelo pergaminho, pelo cajado, pela arma com magia, pelo
 * atalho ou pela roda — e barrada na raiz, no mixin ({@code mixin/magic/AbstractSpellMixin}), e de
 * novo aqui, no evento de antes do lancamento. O que sobra sao as outras portas:
 *
 * <ul>
 *   <li><b>A roda e a barra de magias.</b> Ficam vazias: nenhuma magia de livro, cajado ou arma
 *       aparece para escolher — a roda nem abre, e o atalho de lancar nao tem o que lancar.</li>
 *   <li><b>Objeto magico na mao.</b> Cajado dos Nove, Vendaval do Crepusculo, Convocador de
 *       Almas, Nucleo Energizado, tinturas, elixires, cha de conjurador, pergaminho, manuscrito e
 *       livros de lore: nada responde ao clique de quem nao e mago. O que e objeto comum continua
 *       sendo — besta, bussola, mapa, chave de cofre, disco, comida, armadura, bloco de construir e
 *       ovo de spawn. E material (tinta, essencia, orbe) nem e barrado: clicar com ele ja nao faz
 *       nada.</li>
 *   <li><b>Joia e livro de magia no corpo.</b> Anel, amuleto, talisma e livro de magia nao ficam
 *       nas vagas do Curios de quem nao e mago: nao entram, e o que ja estava volta para a
 *       mochila.</li>
 *   <li><b>Mesas de magia.</b> Mesa de inscricao, forja de pergaminho, bigorna arcana, caldeirao
 *       alquimico, caldeirao de sangue, moldura de portal e runa de alma nao abrem.</li>
 * </ul>
 *
 * <p>Armas e armaduras do mod continuam servindo como armas e armaduras: bater e se proteger nao e
 * magia. O que elas tiverem de magico (a magia gravada numa espada, por exemplo) passa pela regra
 * do lancamento e para ali.
 */
final class MagicRules {
    static final String IRONS = "irons_spellbooks";

    /** Os blocos que fazem magia: as mesas, o caldeirao, o portal e a runa. */
    private static final Set<String> DEVICES = Set.of(
            "inscription_table", "inscription_table_placeholder", "scroll_forge", "arcane_anvil",
            "alchemist_cauldron", "blood_cauldron", "portal_frame", "pocket_dimension_portal_frame",
            "cinderous_soul_rune");
    /** Chaves dos cofres das estruturas: abrir cofre nao e magia. */
    private static final Set<String> MUNDANE = Set.of("decrepit_key", "bone_key");

    private static final String CANNOT_USE = "nexuscore.magic.cannot_use";
    private static final String CANNOT_READ = "nexuscore.magic.cannot_read";
    private static final String CANNOT_DEVICE = "nexuscore.magic.cannot_device";
    private static final String CANNOT_WEAR = "nexuscore.magic.cannot_wear";
    /**
     * O objeto faz alguma coisa ao clique (sobrescreve {@code use}). Tinta, essencia e orbe nao fazem
     * nada ao clique; barrar e avisar ali seria so fumaca a cada clique sem motivo.
     */
    private static final ClassValue<Boolean> ACTS_ON_USE = overrides("use", Level.class, Player.class, InteractionHand.class);
    /** O objeto age sozinho numa criatura (sobrescreve {@code interactLivingEntity}). */
    private static final ClassValue<Boolean> ACTS_ON_CREATURES = overrides("interactLivingEntity",
            ItemStack.class, Player.class, LivingEntity.class, InteractionHand.class);
    /** De quanto em quanto tempo as vagas do Curios e o lancamento em curso sao conferidos. */
    private static final int CHECK_TICKS = 20;

    static void register(IEventBus game) {
        // HIGHEST: o "nao" sai antes de qualquer outra coisa reagir; os ouvintes do proprio Iron's
        // Spells nem chegam a ver o evento cancelado.
        game.addListener(EventPriority.HIGHEST, SpellPreCastEvent.class, MagicRules::onPreCast);
        game.addListener(EventPriority.HIGHEST, InscribeSpellEvent.class, MagicRules::onInscribe);
        // LOWEST: esvazia depois de todo mundo (addons inclusive) ter posto as suas opcoes.
        game.addListener(EventPriority.LOWEST, SpellSelectionManager.SpellSelectionEvent.class, MagicRules::onSelection);
        game.addListener(EventPriority.HIGHEST, PlayerInteractEvent.RightClickItem.class, MagicRules::onUseItem);
        game.addListener(EventPriority.HIGHEST, PlayerInteractEvent.RightClickBlock.class, MagicRules::onUseBlock);
        game.addListener(EventPriority.HIGHEST, PlayerInteractEvent.EntityInteract.class, MagicRules::onUseOnEntity);
        game.addListener(EventPriority.HIGHEST, LivingEntityUseItemEvent.Start.class, MagicRules::onStartUsing);
        game.addListener(EventPriority.HIGHEST, CurioCanEquipEvent.class, MagicRules::onEquip);
        game.addListener(PlayerTickEvent.Post.class, MagicRules::onPlayerTick);
    }

    // ---- o que e magico ----

    static boolean fromIrons(ItemStack stack) {
        return !stack.isEmpty() && IRONS.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    /** Objeto do Iron's Spells que faz magia ao ser usado. O que e objeto comum fica de fora. */
    static boolean magicItem(ItemStack stack) {
        if (!fromIrons(stack)) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof BlockItem || item instanceof SpawnEggItem || item instanceof ArmorItem
                || item instanceof FurledMapItem || item instanceof WaywardCompass || item instanceof AutoloaderCrossbow
                || stack.has(DataComponents.JUKEBOX_PLAYABLE)
                || MUNDANE.contains(BuiltInRegistries.ITEM.getKey(item).getPath())) {
            return false;
        }
        // Comida e comida; beberagem de alquimista (elixir, cha, tintura) e magia.
        return !stack.has(DataComponents.FOOD) || item instanceof DrinkableItem;
    }

    /** Se a classe (ou uma mae dela abaixo de {@link Item}) reescreve aquele metodo de {@link Item}. */
    private static ClassValue<Boolean> overrides(String name, Class<?>... params) {
        return new ClassValue<>() {
            @Override
            protected Boolean computeValue(Class<?> type) {
                try {
                    return type.getMethod(name, params).getDeclaringClass() != Item.class;
                } catch (NoSuchMethodException e) {
                    return true;
                }
            }
        };
    }

    private static boolean magicDevice(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return IRONS.equals(id.getNamespace()) && DEVICES.contains(id.getPath());
    }

    /** Pergaminho e livro para ler: quem nao sabe magia ve as letras dancarem. */
    private static boolean readable(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof Scroll || item instanceof ReadableLoreItem || item instanceof ChronicleItem
                || item instanceof ArchevokerLogbookItem || item instanceof EldritchManuscript;
    }

    private static void refuse(Player player, String key) {
        if (player instanceof ServerPlayer server) {
            MagicFx.refuse(server, key);
        }
    }

    // ---- lancar ----

    /**
     * Segunda tranca do lancamento: se o mixin nao estiver la, o evento do proprio mod barra. O
     * {@code /cast} de operador passa, como nas outras trancas.
     */
    private static void onPreCast(SpellPreCastEvent event) {
        if (event.getCastSource() != CastSource.COMMAND && !Mages.isMage(event.getEntity())) {
            event.setCanceled(true);
            refuse(event.getEntity(), SpellGate.CANNOT_CAST);
        }
    }

    /**
     * A lista de magias para escolher — a da roda, a da barra e a que o servidor consulta ao lancar
     * pelo atalho. Vale dos dois lados: vazia no cliente, a roda nem abre; vazia no servidor, o
     * atalho nao acha o que lancar.
     */
    private static void onSelection(SpellSelectionManager.SpellSelectionEvent event) {
        if (!Mages.isMage(event.getEntity())) {
            event.getManager().getAllSpells().clear();
        }
    }

    /** Gravar magia no livro, na mesa de inscricao. */
    private static void onInscribe(InscribeSpellEvent event) {
        if (!Mages.isMage(event.getEntity())) {
            event.setCanceled(true);
            refuse(event.getEntity(), CANNOT_DEVICE);
        }
    }

    // ---- objetos ----

    private static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (magicItem(stack) && ACTS_ON_USE.get(stack.getItem().getClass()) && !Mages.isMage(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            refuse(event.getEntity(), readable(stack) ? CANNOT_READ : CANNOT_USE);
        }
    }

    /**
     * Clique em bloco. Mesa de magia nao abre. Objeto magico na mao nao age no bloco — mas o bloco
     * continua respondendo: quem segura um pergaminho ainda abre a porta.
     */
    private static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (Mages.isMage(player)) {
            return;
        }
        if (magicDevice(event.getLevel().getBlockState(event.getPos()))) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            refuse(player, CANNOT_DEVICE);
            return;
        }
        if (magicItem(event.getItemStack())) {
            event.setUseItem(TriState.FALSE);
        }
    }

    /**
     * Tintura passada num bicho. So barra o objeto que de fato age em criatura (hoje, a tintura de
     * protecao do Nether): cancelar aqui cancela tambem o clique na criatura, e quem segura um
     * pergaminho ainda precisa conseguir negociar com o aldeao e montar no cavalo.
     */
    private static void onUseOnEntity(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (magicItem(stack) && ACTS_ON_CREATURES.get(stack.getItem().getClass()) && !Mages.isMage(event.getEntity())) {
            event.setCanceled(true);
            refuse(event.getEntity(), CANNOT_USE);
        }
    }

    /** Beber e puxar: a lanca, o elixir e o cha nem comecam. */
    private static void onStartUsing(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && magicItem(event.getItem()) && !Mages.isMage(player)) {
            event.setCanceled(true);
        }
    }

    // ---- joias ----

    private static void onEquip(CurioCanEquipEvent event) {
        if (event.getEntity() instanceof Player player && fromIrons(event.getStack()) && !Mages.isMage(player)) {
            event.setEquipResult(TriState.FALSE);
        }
    }

    /**
     * Uma vez por segundo, para quem nao e mago: joia e livro de magia que ja estavam nas vagas
     * voltam para a mochila, e um lancamento que tenha comecado antes de a pessoa perder a magia
     * para no meio.
     */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || (player.tickCount + player.getId()) % CHECK_TICKS != 0 || Mages.isMage(player)) {
            return;
        }
        if (MagicData.getPlayerMagicData(player).isCasting()) {
            Utils.serverSideCancelCast(player);
        }
        boolean[] stripped = {false};
        CuriosApi.getCuriosInventory(player).ifPresent(curios -> curios.getCurios().values().forEach(handler -> {
            IDynamicStackHandler stacks = handler.getStacks();
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                ItemStack stack = stacks.getStackInSlot(slot);
                if (fromIrons(stack)) {
                    stacks.setStackInSlot(slot, ItemStack.EMPTY);
                    player.getInventory().placeItemBackInInventory(stack);
                    stripped[0] = true;
                }
            }
        }));
        if (stripped[0]) {
            refuse(player, CANNOT_WEAR);
        }
    }

    private MagicRules() {}
}
