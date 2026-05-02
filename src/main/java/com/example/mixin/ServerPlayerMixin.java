package com.example.mixin;

import com.example.NarcolepsyTracker;
import net.minecraft.nbt.CompoundTag;
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

        if (timeSinceRest >= 24000) {
            this.oversleepTicks = 0;
        } else {
            this.oversleepTicks++;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onAddAdditionalSaveData(CompoundTag nbt, CallbackInfo ci) {
        nbt.putInt("OversleepTicks", this.oversleepTicks);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onReadAdditionalSaveData(CompoundTag nbt, CallbackInfo ci) {
        nbt.getInt("OversleepTicks").ifPresent(val -> this.oversleepTicks = val);
    }
}
