package com.example;

import com.example.mixin.PhantomAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public class PhantomDazeGoal extends Goal {
    private final Phantom phantom;

    public PhantomDazeGoal(Phantom phantom) {
        this.phantom = phantom;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return ((SmokeDazeable) this.phantom).getDazeTicks() > 0;
    }

    @Override
    public void start() {
    }

    @Override
    public void tick() {
        SmokeDazeable dazeable = (SmokeDazeable) this.phantom;
        BlockPos center = dazeable.getDazeCenter();
        if (center != null) {
            // Circle slowly at a very low height for melee attacks
            float angle = this.phantom.tickCount * 0.1f;
            double radius = 3.0;
            Vec3 target = new Vec3(center.getX() + Math.cos(angle) * radius, center.getY() + 1.5, center.getZ() + Math.sin(angle) * radius);
            ((PhantomAccessor) this.phantom).setMoveTargetPoint(target);
        }
    }
}
