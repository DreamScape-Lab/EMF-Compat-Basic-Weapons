package org.dreamscape_lab.emf_compat_basic_weapons.mixin.client;

import net.minecraft.world.InteractionHand;
import org.dreamscape_lab.emf_compat_basic_weapons.client.PolearmAttackGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "punchy.client.state.AttackActionTracker", remap = false)
public class PunchyAttackActionTrackerMixin {

    @Inject(
            method = "recordAttack()V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void emfCompatBasicWeapons$gateMainHandAttack(CallbackInfo ci) {
        if (PolearmAttackGate.shouldSuppressTrigger()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "recordAttack(Lnet/minecraft/world/InteractionHand;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void emfCompatBasicWeapons$gateExplicitHandAttack(
            InteractionHand hand,
            CallbackInfo ci
    ) {
        if (hand == InteractionHand.MAIN_HAND && PolearmAttackGate.shouldSuppressTrigger()) {
            ci.cancel();
        }
    }
}
