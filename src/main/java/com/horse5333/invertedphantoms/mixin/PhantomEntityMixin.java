package com.horse5333.invertedphantoms.mixin;

import com.horse5333.invertedphantoms.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;



/**
 * Adds Group Bravery (vibration sensing), Night Retreat, Smoke Daze, and Enrage
 * to the Phantom using the VibrationSystem pattern from the Warden.
 *
 * VibrationSystem lifecycle:
 *   - Entity.updateDynamicGameEventListener() is called by the world when the entity
 *     is added, moved, or removed, automatically registering the listener.
 *   - We override it to forward calls to our DynamicGameEventListener.
 *   - VibrationSystem.Ticker.tick() is called each server tick.
 *
 * Despawn safety for night retreat:
 *   - PhantomNightRetreatGoal pushes the phantom's moveTargetPoint 80 blocks
 *     above its current Y each tick, causing continuous ascent.
 *   - Vanilla hard-despawns mobs at 128 block radius from any player.
 *   - The ascending phantom naturally enters that range and despawns — no
 *     manual discard() is needed.
 */
@Mixin(Phantom.class)
public abstract class PhantomEntityMixin implements SmokeDazeable, VibrationSystem {

    // ── Existing smoke/daze/enrage state ─────────────────────────────────────
    @Unique private int dazeTicks = 0;
    @Unique private int smokeImmunityTicks = 0;
    @Unique private BlockPos dazeCenter = null;
    @Unique private int enrageApproachTicks = 0;
    @Unique private int enrageFrenzyTicks = 0;

    // ── Group bravery (noise-scared) state ───────────────────────────────────
    @Unique private int scaredTicks = 0;
    @Unique private Vec3 scareSourcePos = null;

    // ── VibrationSystem plumbing (mirrors Warden pattern) ────────────────────
    @Unique private final VibrationSystem.Data vibrationData = new VibrationSystem.Data();
    /**
     * Lazy-init so the Phantom is fully constructed when passed to the callback.
     * This is the real listener registered with the world via DynamicGameEventListener.
     */
    @Unique private DynamicGameEventListener<VibrationSystem.Listener> dynamicVibrationListener = null;

    @Unique
    private DynamicGameEventListener<VibrationSystem.Listener> getDynamicListener() {
        if (this.dynamicVibrationListener == null) {
            this.dynamicVibrationListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
        }
        return this.dynamicVibrationListener;
    }

    // VibrationSystem contract ─────────────────────────────────────────────────
    @Override
    public VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return new PhantomVibrationCallback((Phantom) (Object) this);
    }

    // ── SmokeDazeable getters/setters ─────────────────────────────────────────
    @Override public int getDazeTicks() { return this.dazeTicks; }
    @Override public void setDazeTicks(int ticks) { this.dazeTicks = ticks; }
    @Override public BlockPos getDazeCenter() { return this.dazeCenter; }

    @Override public int getEnrageApproachTicks() { return this.enrageApproachTicks; }
    @Override public void setEnrageApproachTicks(int ticks) { this.enrageApproachTicks = ticks; }
    @Override public int getEnrageFrenzyTicks() { return this.enrageFrenzyTicks; }
    @Override public void setEnrageFrenzyTicks(int ticks) { this.enrageFrenzyTicks = ticks; }

    @Override public int getScaredTicks() { return this.scaredTicks; }
    @Override public void setScaredTicks(int ticks) { this.scaredTicks = ticks; }
    @Override public Vec3 getScareSourcePos() { return this.scareSourcePos; }
    @Override public void setScareSourcePos(Vec3 pos) { this.scareSourcePos = pos; }

    // ── TICK injection ────────────────────────────────────────────────────────
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        Phantom phantom = (Phantom) (Object) this;
        if (phantom.level().isClientSide() || !phantom.isAlive()) return;

        if (phantom.level() instanceof ServerLevel serverLevel) {
            // Keep the DynamicGameEventListener registered in the correct chunk section.
            // DynamicGameEventListener.move() is a no-op if the section hasn't changed,
            // so calling it every tick is cheap and handles both initial registration
            // and chunk-section transitions as the phantom moves.
            this.getDynamicListener().move(serverLevel);

            // Advance the vibration pipeline
            VibrationSystem.Ticker.tick(serverLevel, this.vibrationData, this.getVibrationUser());

            // Countdown scared ticks
            if (this.scaredTicks > 0) {
                this.scaredTicks--;
            }

            // Smoke immunity
            if (this.smokeImmunityTicks > 0) {
                this.smokeImmunityTicks--;
            }

            // Daze / enrage timers
            if (this.dazeTicks > 0) {
                this.dazeTicks--;
                if (this.dazeTicks == 0) {
                    this.enrageApproachTicks = 400; // 20 seconds
                }
            } else if (this.enrageFrenzyTicks > 0) {
                this.enrageFrenzyTicks--;
            } else if (this.enrageApproachTicks > 0) {
                this.enrageApproachTicks--;
            }

            // Campfire smoke daze trigger
            if (this.dazeTicks == 0 && this.enrageFrenzyTicks == 0
                    && this.enrageApproachTicks == 0 && this.smokeImmunityTicks == 0) {
                if (serverLevel.getGameRules().get(InvertedPhantomsMod.PHANTOM_BEHAVIOR_TWEAKS)) {
                    boolean isSmokey = false;
                    BlockPos center = phantom.blockPosition();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (CampfireBlock.isSmokeyPos(phantom.level(), center.offset(dx, 0, dz))) {
                                isSmokey = true;
                                break;
                            }
                        }
                        if (isSmokey) break;
                    }

                    if (isSmokey) {
                        this.dazeTicks = 60;
                        this.smokeImmunityTicks = 400;
                        this.dazeCenter = phantom.blockPosition();
                        phantom.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true));
                        phantom.setTarget(null); // Drop the target to abort the current swoop immediately
                    }
                }
            }
        }
    }

    // ── Register new goals ────────────────────────────────────────────────────
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void onRegisterGoals(CallbackInfo ci) {
        Phantom phantom = (Phantom) (Object) this;
        GoalSelector selector = ((MobAccessor) phantom).getGoalSelector();

        // 0: custom overrides (highest priority — overrides vanilla 1/2/3)
        selector.addGoal(0, new PhantomNightRetreatGoal(phantom));
        selector.addGoal(0, new PhantomScaredGoal(phantom));
        selector.addGoal(0, new PhantomDazeGoal(phantom));
        selector.addGoal(0, new PhantomEnragedGoal(phantom));
    }
}
