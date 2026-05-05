package com.horse5333.invertedphantoms;

import com.horse5333.invertedphantoms.mixin.PhantomAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * When it becomes night, phantoms ascend vertically at speed.
 * The vanilla checkDespawn() hard-despawns mobs beyond 128 blocks from
 * any player, so flying straight up will naturally remove them once
 * they're high enough — no manual discard() needed.
 *
 * Safe range check:
 *   - Entity AI runs fine up to simulation-distance (default 10 chunks = 160 blocks)
 *   - Hard despawn at 128 blocks (the getDespawnDistance() for monsters)
 *   - We fly upward; the phantom will be discarded by vanilla at ~128 block radius
 *
 * The phantom flies to retreatY = its current Y + 80, which stays just
 * below the hard-despawn ceiling so AI keeps working the whole time it
 * ascends.  Vanilla's random probabilistic despawn then handles cleanup.
 */
public class PhantomNightRetreatGoal extends Goal {

    // skyDarken goes from 0 (midday) to 11 (full night, ~13000 ticks).
    // 4 is the threshold vanilla uses for "undead burn" — we flip it for night.
    private static final int NIGHT_SKY_DARKEN_THRESHOLD = 4;

    private final Phantom phantom;
    private Vec3 retreatTarget = null;

    public PhantomNightRetreatGoal(Phantom phantom) {
        this.phantom = phantom;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    private boolean isNight() {
        return this.phantom.level().getSkyDarken() >= NIGHT_SKY_DARKEN_THRESHOLD;
    }

    @Override
    public boolean canUse() {
        if (!(this.phantom.level() instanceof ServerLevel serverLevel)) return false;
        if (!serverLevel.getGameRules().get(InvertedPhantomsMod.PHANTOM_BEHAVIOR_TWEAKS)) return false;
        return isNight();
    }

    @Override
    public boolean canContinueToUse() {
        return isNight();
    }

    @Override
    public void start() {
        // Fly to a point 80 blocks above current position — stays under 128 so AI ticks
        Vec3 pos = this.phantom.position();
        this.retreatTarget = new Vec3(pos.x(), pos.y() + 80.0, pos.z());
    }

    @Override
    public void tick() {
        // Keep pushing the target upward as the phantom climbs
        Vec3 pos = this.phantom.position();
        this.retreatTarget = new Vec3(pos.x(), pos.y() + 80.0, pos.z());
        ((PhantomAccessor) this.phantom).setMoveTargetPoint(this.retreatTarget);
    }

    @Override
    public void stop() {
        this.retreatTarget = null;
    }
}
