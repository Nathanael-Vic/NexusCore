package com.nexussmp.nexuscore.mixin.magic;

import com.nexussmp.nexuscore.magic.SpellGate;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Toda magia do Iron's Spells, para quem nao e mago, vira uma magia que a pessoa nunca aprendeu.
 *
 * <p>E a mesma regra que o mod ja usa nas magias eldritch, estendida a todas: o nome embaralhado e
 * os numeros escondidos ({@code obfuscateStats} — e por ele que passam o livro, o pergaminho, a roda,
 * a barra e a mesa de inscricao), nada de lancar ({@code canBeCastedBy}, a porta de todo lancamento
 * de jogador) e nada de fazer pergaminho ({@code canBeCraftedBy}).
 *
 * <p>Tres portas de uma classe so, e todas recebem a pessoa: nao existe caminho de lancar que passe
 * sem perguntar quem e. O {@code castSpell} e a ultima tranca, para o lancamento que ja estava em
 * curso quando alguem perdeu a magia — o comando {@code /cast} da moderacao continua passando.
 */
@Mixin(AbstractSpell.class)
public abstract class AbstractSpellMixin {

    @Inject(method = "obfuscateStats", at = @At("HEAD"), cancellable = true)
    private void nexus$unreadable(Player player, CallbackInfoReturnable<Boolean> result) {
        if (player != null && SpellGate.hidesSpells(player)) {
            result.setReturnValue(true);
        }
    }

    @Inject(method = "canBeCastedBy", at = @At("HEAD"), cancellable = true)
    private void nexus$onlyMages(int level, CastSource source, MagicData magicData, Player player,
                                 CallbackInfoReturnable<CastResult> result) {
        if (player != null && source != CastSource.COMMAND && !SpellGate.mayCast(player)) {
            result.setReturnValue(new CastResult(CastResult.Type.FAILURE, SpellGate.refuse(player)));
        }
    }

    @Inject(method = "canBeCraftedBy", at = @At("HEAD"), cancellable = true)
    private void nexus$onlyMagesCraft(Player player, CallbackInfoReturnable<Boolean> result) {
        if (player != null && !SpellGate.mayCast(player)) {
            result.setReturnValue(false);
        }
    }

    @Inject(method = "castSpell", at = @At("HEAD"), cancellable = true)
    private void nexus$lastLock(Level level, int spellLevel, ServerPlayer player, CastSource source,
                                boolean triggerCooldown, CallbackInfo callback) {
        if (source != CastSource.COMMAND && !SpellGate.mayCast(player)) {
            callback.cancel();
        }
    }
}
