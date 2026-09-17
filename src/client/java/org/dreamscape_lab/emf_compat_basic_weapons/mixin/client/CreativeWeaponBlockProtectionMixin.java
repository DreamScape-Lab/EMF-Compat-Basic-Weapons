package org.dreamscape_lab.emf_compat_basic_weapons.mixin.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class CreativeWeaponBlockProtectionMixin {

    @Unique
    private static final TagKey<Item> CLUBS = weaponTag("clubs");

    @Unique
    private static final TagKey<Item> HAMMERS = weaponTag("hammers");

    @Unique
    private static final TagKey<Item> QUARTERSTAVES = weaponTag("quarterstaves");

    @Inject(method = "canDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void emfCompatBasicWeapons$protectCreativeBlocks(
            ItemStack itemStack,
            BlockState state,
            Level level,
            BlockPos pos,
            LivingEntity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof Player player
                && player.getAbilities().instabuild
                && (itemStack.is(CLUBS) || itemStack.is(HAMMERS) || itemStack.is(QUARTERSTAVES))) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static TagKey<Item> weaponTag(String path) {
        return TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath("c", "tools/" + path)
        );
    }
}
