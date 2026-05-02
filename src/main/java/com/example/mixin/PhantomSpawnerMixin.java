package com.example.mixin;

import com.example.NarcolepsyTracker;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getSkyDarken()I"))
    private int redirectSkyDarken(ServerLevel level) {
        int original = level.getSkyDarken();
        return original < 5 ? 5 : 0;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/stats/ServerStatsCounter;getValue(Lnet/minecraft/stats/Stat;)I"))
    private int redirectGetValue(ServerStatsCounter instance, Stat<?> stat, @Local ServerPlayer player) {
        return ((NarcolepsyTracker) player).getOversleepTicks();
    }
}
