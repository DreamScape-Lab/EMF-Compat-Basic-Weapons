package org.dreamscape_lab.emf_compat_basic_weapons.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.dreamscape_lab.emf_compat_basic_weapons.client.Emf_compat_basic_weaponsClient;
import org.dreamscape_lab.emf_compat_basic_weapons.client.PikeFaBlend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import strm.emfcompat.core.PoseManager;
import strm.emfcompat.core.PoseSnapshot;
import strm.emfcompat.core.SavedPoses;

import java.util.UUID;

@Mixin(value = PlayerModel.class, priority = 2500)
public class PlayerModelMixin {

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("RETURN")
    )
    private void emfCompatBasicWeapons$capturePikePose(AvatarRenderState state, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUUID();
        PlayerModel model = (PlayerModel) (Object) this;

        boolean rightPike = PikeFaBlend.isPike(state.rightHandItemStack);
        boolean leftPike = PikeFaBlend.isPike(state.leftHandItemStack);
        ItemStack attackingStack = state.attackArm == HumanoidArm.RIGHT
                ? state.rightHandItemStack
                : state.leftHandItemStack;
        boolean pikeAttack = state.attackTime > 0.0F && PikeFaBlend.isPike(attackingStack);

        PikeFaBlend.updateEquipTransition(uuid, HumanoidArm.RIGHT, rightPike, state.ageInTicks);
        PikeFaBlend.updateEquipTransition(uuid, HumanoidArm.LEFT, leftPike, state.ageInTicks);

        if (pikeAttack || (!state.isUsingItem && (rightPike || leftPike))) {
            applySpearStylePikePose(model, state, uuid, rightPike, leftPike, pikeAttack);

            PoseManager.savePoses(
                    uuid,
                    Emf_compat_basic_weaponsClient.POSE_SOURCE,
                    leftPike || (pikeAttack && state.attackArm == HumanoidArm.LEFT)
                            ? new PoseSnapshot(model.leftArm) : null,
                    rightPike || (pikeAttack && state.attackArm == HumanoidArm.RIGHT)
                            ? new PoseSnapshot(model.rightArm) : null,
                    null
            );
            return;
        }

        PoseManager.clearPoses(uuid, Emf_compat_basic_weaponsClient.POSE_SOURCE);
    }

    @Unique
    private static void applySpearStylePikePose(
            PlayerModel model,
            AvatarRenderState state,
            UUID uuid,
            boolean rightPike,
            boolean leftPike,
            boolean pikeAttack
    ) {
        if (rightPike) {
            blendToSpearHold(
                    model.rightArm,
                    model.head,
                    true,
                    state.rightHandItemStack,
                    state,
                    uuid,
                    HumanoidArm.RIGHT,
                    pikeAttack && state.attackArm == HumanoidArm.RIGHT
            );
        }
        if (leftPike) {
            blendToSpearHold(
                    model.leftArm,
                    model.head,
                    false,
                    state.leftHandItemStack,
                    state,
                    uuid,
                    HumanoidArm.LEFT,
                    pikeAttack && state.attackArm == HumanoidArm.LEFT
            );
        }

        if (pikeAttack) {
            SpearAnimations.thirdPersonAttackHand(model, state);
        }
    }

    @Unique
    private static void blendToSpearHold(
            ModelPart arm,
            ModelPart head,
            boolean right,
            ItemStack stack,
            AvatarRenderState state,
            UUID uuid,
            HumanoidArm humanoidArm,
            boolean attacking
    ) {
        float fromX = arm.xRot;
        float fromY = arm.yRot;
        float fromZ = arm.zRot;

        SpearAnimations.thirdPersonHandUse(arm, head, right, stack, state);

        float weight = attacking ? 1.0F : PikeFaBlend.actionWeight(uuid, humanoidArm, state.ageInTicks);
        PikeFaBlend.blendArmRotation(arm, fromX, fromY, fromZ, weight);
    }

    @Inject(
            method = "translateToHand(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("HEAD")
    )
    private void emfCompatBasicWeapons$restorePikeArmForItem(
            net.minecraft.client.renderer.entity.state.EntityRenderState state,
            HumanoidArm arm,
            PoseStack poseStack,
            CallbackInfo ci
    ) {
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(avatarState.id);
        if (!(entity instanceof Player player)) {
            return;
        }

        boolean rightPike = PikeFaBlend.isPike(avatarState.rightHandItemStack);
        boolean leftPike = PikeFaBlend.isPike(avatarState.leftHandItemStack);
        ItemStack attackingStack = avatarState.attackArm == HumanoidArm.RIGHT
                ? avatarState.rightHandItemStack
                : avatarState.leftHandItemStack;
        boolean pikeAttack = avatarState.attackTime > 0.0F && PikeFaBlend.isPike(attackingStack);
        boolean armHoldsPike = arm == HumanoidArm.RIGHT ? rightPike : leftPike;
        if (!pikeAttack && (avatarState.isUsingItem || !armHoldsPike)) {
            return;
        }

        SavedPoses saved = PoseManager.getSavedPoses(player.getUUID());
        if (saved == null) {
            return;
        }

        PoseSnapshot snapshot = arm == HumanoidArm.LEFT ? saved.leftArm() : saved.rightArm();
        if (snapshot == null) {
            return;
        }

        PlayerModel model = (PlayerModel) (Object) this;
        ModelPart modelArm = arm == HumanoidArm.LEFT ? model.leftArm : model.rightArm;

        float freshX = modelArm.xRot;
        float freshY = modelArm.yRot;
        float freshZ = modelArm.zRot;

        snapshot.applyRotation(modelArm);

        float blend = PikeFaBlend.actionWeight(player.getUUID(), arm, avatarState.ageInTicks);
        if (pikeAttack && arm == avatarState.attackArm) {
            blend = 1.0F;
        }

        PikeFaBlend.blendArmRotation(modelArm, freshX, freshY, freshZ, blend);
    }
}
