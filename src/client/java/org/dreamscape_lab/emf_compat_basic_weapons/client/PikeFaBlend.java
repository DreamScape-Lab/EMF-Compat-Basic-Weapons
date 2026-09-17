package org.dreamscape_lab.emf_compat_basic_weapons.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PikeFaBlend {

    public static final float BLEND_RATE = 8.0F;

    public static final TagKey<Item> PIKES = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("c", "tools/pikes")
    );

    private static final Map<ArmKey, Float> EQUIP_STARTS = new HashMap<>();

    private PikeFaBlend() {
    }

    public static boolean isPike(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(PIKES);
    }

    public static void updateEquipTransition(UUID uuid, HumanoidArm arm, boolean holding, float ageInTicks) {
        ArmKey key = new ArmKey(uuid, arm);
        if (holding) {
            EQUIP_STARTS.putIfAbsent(key, ageInTicks);
        } else {
            EQUIP_STARTS.remove(key);
        }
    }

    public static float actionWeight(UUID uuid, HumanoidArm arm, float ageInTicks) {
        Float start = EQUIP_STARTS.get(new ArmKey(uuid, arm));
        if (start == null) {
            return 1.0F;
        }

        float seconds = Math.max(0.0F, ageInTicks - start) / 20.0F;
        float drag = 1.0F - (float) Math.exp(-BLEND_RATE * seconds);
        drag = Mth.clamp(drag * 1.02F, 0.0F, 1.0F);
        float actionT = drag + Mth.sin(drag * Mth.PI) / 2.5F;
        return actionT * actionT;
    }

    public static void blendArmRotation(ModelPart arm, float fromX, float fromY, float fromZ, float weight) {
        arm.xRot = Mth.rotLerpRad(weight, fromX, arm.xRot);
        arm.yRot = Mth.rotLerpRad(weight, fromY, arm.yRot);
        arm.zRot = Mth.rotLerpRad(weight, fromZ, arm.zRot);
    }

    private record ArmKey(UUID uuid, HumanoidArm arm) {
    }
}
