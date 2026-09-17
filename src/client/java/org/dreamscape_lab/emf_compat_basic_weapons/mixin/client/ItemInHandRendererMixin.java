package org.dreamscape_lab.emf_compat_basic_weapons.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.dreamscape_lab.emf_compat_basic_weapons.client.PikeFaBlend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/** Lets Punchy own FP pikes; priority 500 so we cancel before BW's extra draw. */
@Mixin(value = ItemInHandRenderer.class, priority = 500)
public class ItemInHandRendererMixin {

    @Unique
    private static Method emfCompatBasicWeapons$punchyEnabledMethod;
    @Unique
    private static boolean emfCompatBasicWeapons$punchyMethodResolved;

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void emfCompatBasicWeapons$letPunchyOwnPike(
            AbstractClientPlayer player,
            float frameInterp,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack itemStack,
            float equipProgress,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != player || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        if (!itemStack.isEmpty() && itemStack.is(PikeFaBlend.PIKES) && emfCompatBasicWeapons$isPunchyEnabled()) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean emfCompatBasicWeapons$isPunchyEnabled() {
        if (!FabricLoader.getInstance().isModLoaded("punchy")) {
            return false;
        }

        if (!emfCompatBasicWeapons$punchyMethodResolved) {
            emfCompatBasicWeapons$punchyMethodResolved = true;
            try {
                Class<?> config = Class.forName("punchy.config.PunchyConfig");
                emfCompatBasicWeapons$punchyEnabledMethod = config.getMethod("isModEnabled");
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        }

        if (emfCompatBasicWeapons$punchyEnabledMethod == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(emfCompatBasicWeapons$punchyEnabledMethod.invoke(null));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
