package com.horse5333.invertedphantoms;

import com.horse5333.invertedphantoms.mixin.PhantomAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Makes a scared phantom flee upward and away from the noise source.
 * Triggered by PhantomVibrationCallback setting scaredTicks > 0.
 *
 * The phantom flies at a 60-degree upward angle away from the sound source,
 * aiming to rise ~15 blocks above its current position while running away.
 * This keeps it well within AI range (hard despawn at 128 blocks).
 */
public class PhantomScaredGoal extends Goal {

    private final Phantom phantom;
    private Vec3 retreatTarget = null;

    public PhantomScaredGoal(Phantom phantom) {
        this.phantom = phantom;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(this.phantom.level() instanceof ServerLevel serverLevel)) return false;
        if (!serverLevel.getGameRules().get(InvertedPhantomsMod.PHANTOM_BEHAVIOR_TWEAKS)) return false;
        return ((SmokeDazeable) this.phantom).getScaredTicks() > 0;
    }

    @Override
    public boolean canContinueToUse() {
        return ((SmokeDazeable) this.phantom).getScaredTicks() > 0;
    }

    @Override
    public void start() {
        // Calculate retreat point: move horizontally away from scare source + rise 15 blocks
        SmokeDazeable state = (SmokeDazeable) this.phantom;
        Vec3 source = state.getScareSourcePos();
        Vec3 phantomPos = this.phantom.position();

        Vec3 awayDir;
        if (source != null) {
            // Direction from noise source toward phantom (flee direction)
            awayDir = phantomPos.subtract(source).normalize();
            // If the phantom IS the source (shouldn't happen but safety)
            if (awayDir.lengthSqr() < 0.01) {
                awayDir = new Vec3(0, 1, 0);
            }
        } else {
            awayDir = new Vec3(0, 1, 0);
        }

        // Flee 20 blocks horizontally + 15 blocks up
        this.retreatTarget = phantomPos
                .add(awayDir.x() * 20.0, 15.0, awayDir.z() * 20.0);
    }

    @Override
    public void tick() {
        if (this.retreatTarget != null) {
            ((PhantomAccessor) this.phantom).setMoveTargetPoint(this.retreatTarget);
        }
    }

    @Override
    public void stop() {
        this.retreatTarget = null;
    }
}
