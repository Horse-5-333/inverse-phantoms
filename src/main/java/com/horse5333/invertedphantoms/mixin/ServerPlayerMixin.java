package com.horse5333.invertedphantoms.mixin;

import com.horse5333.invertedphantoms.NarcolepsyTracker;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements NarcolepsyTracker {

    @Shadow public abstract ServerStatsCounter getStats();

    @Unique
    private int oversleepTicks = 0;

    @Override
    public int getOversleepTicks() {
        return this.oversleepTicks;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        int timeSinceRest = this.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));

        // Narcolepsy: if the player has slept recently (within 1 day), they are
        // "oversleeping" — accumulate ticks unboundedly so the spawner probability
        // eventually fires. If they've stayed awake 1+ days, reset (normal behaviour).
        if (timeSinceRest >= 24000) {
            this.oversleepTicks = 0;
        } else {
            this.oversleepTicks++;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onAddAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
        output.putInt("OversleepTicks", this.oversleepTicks);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onReadAdditionalSaveData(ValueInput input, CallbackInfo ci) {
        input.getInt("OversleepTicks").ifPresent(val -> this.oversleepTicks = val);
    }
}
