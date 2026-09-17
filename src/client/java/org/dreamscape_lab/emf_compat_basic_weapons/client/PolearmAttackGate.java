package org.dreamscape_lab.emf_compat_basic_weapons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/** Blocks Punchy from restarting a long polearm swing while still looking at a block. */
public final class PolearmAttackGate {

    private static final int FALLBACK_ATTACK_TICKS = 20;

    private static final TagKey<Item> GLAIVES = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("c", "tools/glaives")
    );
    private static final TagKey<Item> QUARTERSTAVES = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("c", "tools/quarterstaves")
    );
    private static final TagKey<Item> SPEARS = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("c", "tools/spears")
    );

    private static boolean attackCycleActive;
    private static boolean attackClipObserved;
    private static Object observedAttackClip;
    private static Object clipBeforeAttack;
    private static float timeBeforeAttack;
    private static long clientTick;
    private static long attackAcceptedTick;

    private static boolean punchyReflectionResolved;
    private static Field punchyHandlersField;
    private static Method punchyIsPlayingMethod;
    private static Method punchyGetCurrentClipMethod;
    private static Method punchyGetCurrentTimeMethod;
    private static Method punchyGetClipLengthMethod;

    private PolearmAttackGate() {
    }

    /** @return true if Punchy should ignore this attack press */
    public static boolean shouldSuppressTrigger() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isGatedWeaponAtBlock(minecraft) || !minecraft.options.keyAttack.isDown()) {
            resetCycle();
            return false;
        }

        clientTick = minecraft.level.getGameTime();
        if (!attackCycleActive || hasAttackClipFinished()) {
            beginCycle();
            return false;
        }

        return true;
    }

    public static void tick(Minecraft minecraft) {
        clientTick++;
        if (minecraft == null
                || minecraft.options == null
                || !minecraft.options.keyAttack.isDown()
                || !isGatedWeaponAtBlock(minecraft)) {
            resetCycle();
            return;
        }

        observeAttackClip();
    }

    private static boolean isGatedWeaponAtBlock(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.hitResult == null) {
            return false;
        }

        ItemStack stack = minecraft.player.getMainHandItem();
        return !stack.isEmpty()
                && (stack.is(PikeFaBlend.PIKES)
                        || stack.is(GLAIVES)
                        || stack.is(QUARTERSTAVES)
                        || stack.is(SPEARS))
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK;
    }

    private static void beginCycle() {
        attackCycleActive = true;
        attackClipObserved = false;
        observedAttackClip = null;
        attackAcceptedTick = clientTick;

        PunchyAnimationState state = readPunchyAnimationState();
        clipBeforeAttack = state == null ? null : state.clip();
        timeBeforeAttack = state == null ? 0.0F : state.currentTime();
    }

    private static void resetCycle() {
        attackCycleActive = false;
        attackClipObserved = false;
        observedAttackClip = null;
        clipBeforeAttack = null;
        timeBeforeAttack = 0.0F;
        attackAcceptedTick = 0L;
    }

    private static void observeAttackClip() {
        if (!attackCycleActive || attackClipObserved || clientTick <= attackAcceptedTick) {
            return;
        }

        PunchyAnimationState state = readPunchyAnimationState();
        boolean newlyStarted = state != null
                && state.playing()
                && state.clip() != null
                && (state.clip() != clipBeforeAttack || state.currentTime() < timeBeforeAttack);
        if (newlyStarted) {
            attackClipObserved = true;
            observedAttackClip = state.clip();
        }
    }

    private static boolean hasAttackClipFinished() {
        observeAttackClip();

        PunchyAnimationState state = readPunchyAnimationState();
        if (attackClipObserved && state != null) {
            return !state.playing()
                    || state.clip() != observedAttackClip
                    || state.currentTime() >= state.clipLength();
        }

        return clientTick - attackAcceptedTick >= FALLBACK_ATTACK_TICKS;
    }

    private static PunchyAnimationState readPunchyAnimationState() {
        if (!resolvePunchyReflection()) {
            return null;
        }

        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return null;
            }

            Object handlersObject = punchyHandlersField.get(null);
            if (!(handlersObject instanceof Map<?, ?> handlers)) {
                return null;
            }

            HumanoidArm mainHandArm = minecraft.player.getMainArm();
            Object poseHandler = handlers.get(mainHandArm);
            if (poseHandler == null) {
                return null;
            }

            boolean playing = Boolean.TRUE.equals(punchyIsPlayingMethod.invoke(poseHandler));
            Object clip = punchyGetCurrentClipMethod.invoke(poseHandler);
            float currentTime = ((Number) punchyGetCurrentTimeMethod.invoke(poseHandler)).floatValue();
            float clipLength = clip == null
                    ? 0.0F
                    : ((Number) punchyGetClipLengthMethod.invoke(clip)).floatValue();
            return new PunchyAnimationState(playing, clip, currentTime, clipLength);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    private static boolean resolvePunchyReflection() {
        if (punchyReflectionResolved) {
            return punchyHandlersField != null;
        }

        punchyReflectionResolved = true;
        try {
            Class<?> itemAnimationManager =
                    Class.forName("punchy.client.animation.ItemAnimationManager");
            punchyHandlersField = itemAnimationManager.getDeclaredField("HANDLERS");
            punchyHandlersField.setAccessible(true);

            Class<?> poseHandlerClass = Class.forName("punchy.client.animation.PoseHandler");
            punchyIsPlayingMethod = poseHandlerClass.getMethod("isPlaying");
            punchyGetCurrentClipMethod = poseHandlerClass.getMethod("getCurrentClip");
            punchyGetCurrentTimeMethod = poseHandlerClass.getMethod("getCurrentTime");

            Class<?> animationClipClass = Class.forName("punchy.client.animation.data.AnimationClip");
            punchyGetClipLengthMethod = animationClipClass.getMethod("getLength");
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            punchyHandlersField = null;
            return false;
        }
    }

    private record PunchyAnimationState(
            boolean playing,
            Object clip,
            float currentTime,
            float clipLength
    ) {
    }
}
