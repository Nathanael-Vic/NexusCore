package com.nexussmp.nexuscore.monolith.client.jei;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.monolith.MonolithSites;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;

/** O Codex e os fragmentos de pesquisa do Ancient Remnants somem da lista do JEI. */
@JeiPlugin
public final class MonolithJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = NexusCore.id("monolith");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (!ModList.get().isLoaded(MonolithSites.NAMESPACE)) {
            return;
        }
        List<ItemStack> items = BuiltInRegistries.ITEM.entrySet().stream()
                .filter(entry -> MonolithSites.NAMESPACE.equals(entry.getKey().location().getNamespace()))
                .map(entry -> new ItemStack(entry.getValue()))
                .toList();
        if (!items.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, items);
        }
    }
}
