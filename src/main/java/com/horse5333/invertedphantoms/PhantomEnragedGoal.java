package com.horse5333.invertedphantoms;

import com.horse5333.invertedphantoms.mixin.PhantomAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public class PhantomEnragedGoal extends Goal {
    private final Phantom phantom;
    private int successfulHits = 0;

    public PhantomEnragedGoal(Phantom phantom) {
        this.phantom = phantom;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(this.phantom.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return false;
        if (!serverLevel.getGameRules().get(InvertedPhantomsMod.PHANTOM_BEHAVIOR_TWEAKS)) return false;
        SmokeDazeable state = (SmokeDazeable) this.phantom;
        return (state.getEnrageApproachTicks() > 0 || state.getEnrageFrenzyTicks() > 0) && this.phantom.getTarget() != null;
    }

    @Override
    public void start() {
    }

    @Override
    public void tick() {
        SmokeDazeable state = (SmokeDazeable) this.phantom;
        LivingEntity target = this.phantom.getTarget();

        if (target != null) {
            // Constantly set the target point directly to the player's center
            ((PhantomAccessor) this.phantom).setMoveTargetPoint(new Vec3(target.getX(), target.getY(0.5), target.getZ()));

            // Check collision
            if (this.phantom.getBoundingBox().inflate(0.2F).intersects(target.getBoundingBox())) {
                // If this is the FIRST time hitting the player during approach, start the 5-second frenzy
                if (state.getEnrageFrenzyTicks() == 0 && state.getEnrageApproachTicks() > 0) {
                    state.setEnrageApproachTicks(0); // End approach
                    state.setEnrageFrenzyTicks(100); // Start 5 seconds of frenzy
                    this.successfulHits = 0; // Reset hit counter
                }

                // Deal damage
                if (this.phantom.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    boolean hurt = this.phantom.doHurtTarget(serverLevel, target);
                    if (hurt) {
                        this.successfulHits++;
                        if (this.successfulHits >= 3) {
                            // End the frenzy early after 3 hits
                            state.setEnrageFrenzyTicks(0);
                        }
                    }
                }
            }
        }
    }
}
