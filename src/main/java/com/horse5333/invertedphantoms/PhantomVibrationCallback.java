package com.horse5333.invertedphantoms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Implements VibrationSystem.User — the actual reaction logic when a vibration arrives.
 *
 * Frequency scale (from VibrationSystem.VIBRATION_FREQUENCY_FOR_EVENT):
 *   1  = step/swim/flap
 *   2  = hit ground / projectile land
 *   6  = entity interact / shear / mount
 *   12 = block destroy
 *   15 = entity die / explode
 */
public class PhantomVibrationCallback implements VibrationSystem.User {

    private static final int LISTEN_RANGE = 40;

    // Bravery thresholds
    private static final int THRESHOLD_SMALL  = 1;  // 1-3 phantoms: any noise
    private static final int THRESHOLD_MEDIUM = 6;  // 4-6 phantoms: moderate noise
    private static final int THRESHOLD_IMMUNE = 16; // 7+ phantoms: immune (impossible)

    private final Phantom phantom;

    public PhantomVibrationCallback(Phantom phantom) {
        this.phantom = phantom;
    }

    @Override
    public int getListenerRadius() {
        return LISTEN_RANGE;
    }

    @Override
    public PositionSource getPositionSource() {
        return new EntityPositionSource(this.phantom, this.phantom.getEyeHeight());
    }

    @Override
    public boolean canReceiveVibration(ServerLevel level, BlockPos pos,
                                       Holder<GameEvent> event, GameEvent.Context context) {
        if (!level.getGameRules().get(InvertedPhantomsMod.PHANTOM_BEHAVIOR_TWEAKS)) return false;
        // Ignore our own footsteps
        if (context.sourceEntity() == this.phantom) return false;
        return true;
    }

    @Override
    public void onReceiveVibration(ServerLevel level, BlockPos pos,
                                   Holder<GameEvent> event,
                                   @Nullable Entity sourceEntity,
                                   @Nullable Entity projectileOwner,
                                   float receivingDistance) {
        SmokeDazeable state = (SmokeDazeable) this.phantom;

        // Already scared — don't stack
        if (state.getScaredTicks() > 0) return;

        // Count nearby phantoms (including self) within 40 blocks
        int groupSize = level.getEntitiesOfClass(
                Phantom.class,
                this.phantom.getBoundingBox().inflate(LISTEN_RANGE),
                Entity::isAlive
        ).size();

        int braveryThreshold;
        if (groupSize >= 7) {
            braveryThreshold = THRESHOLD_IMMUNE;
        } else if (groupSize >= 4) {
            braveryThreshold = THRESHOLD_MEDIUM;
        } else {
            braveryThreshold = THRESHOLD_SMALL;
        }

        // Look up the vibration frequency for this game event
        int frequency = VibrationSystem.getGameEventFrequency(event);
        if (frequency >= braveryThreshold) {
            state.setScaredTicks(100); // 5 seconds
            state.setScareSourcePos(Vec3.atCenterOf(pos));
        }
    }
}
