package com.nexussmp.nexuscore.magic.client.jei;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.magic.Mages;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * O JEI tem, para cada magia, uma pagina com a descricao dela presa ao pergaminho — e ali ninguem
 * embaralha nada, porque o texto e montado uma vez so, quando o JEI carrega. Para quem nao e mago,
 * essas paginas somem; voltam quando a pessoa vira maga.
 *
 * <p>So o JEI carrega esta classe (pela anotacao), e o {@link com.nexussmp.nexuscore.magic.client.MagicClient}
 * so a chama depois de ver que o JEI esta instalado.
 */
@JeiPlugin
public final class MagicJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = NexusCore.id("magic");
    private static final ResourceLocation SCROLL = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "scroll");

    private static IJeiRuntime runtime;
    /** O que ja esta aplicado no JEI de agora; um runtime novo comeca com tudo a mostra. */
    private static boolean hidden;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        hidden = false;
        refresh();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        hidden = false;
    }

    /** Esconde ou mostra as paginas das magias conforme a pessoa sabe ou nao magia. */
    public static void refresh() {
        IJeiRuntime current = runtime;
        if (current == null || !ModList.get().isLoaded("irons_spellbooks")) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        boolean hide = player != null && !Mages.isMage(player);
        if (hide == hidden) {
            return;
        }
        IRecipeManager recipes = current.getRecipeManager();
        List<IJeiIngredientInfoRecipe> pages = recipes.createRecipeLookup(RecipeTypes.INFORMATION)
                .includeHidden()
                .get()
                .filter(MagicJeiPlugin::isSpellPage)
                .toList();
        if (hide) {
            recipes.hideRecipes(RecipeTypes.INFORMATION, pages);
        } else {
            recipes.unhideRecipes(RecipeTypes.INFORMATION, pages);
        }
        hidden = hide;
    }

    /** A pagina de uma magia e a que o Iron's Spells prende aos pergaminhos daquela magia. */
    private static boolean isSpellPage(IJeiIngredientInfoRecipe page) {
        for (ITypedIngredient<?> ingredient : page.getIngredients()) {
            ItemStack stack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
            if (!stack.isEmpty() && SCROLL.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return true;
            }
        }
        return false;
    }
}
