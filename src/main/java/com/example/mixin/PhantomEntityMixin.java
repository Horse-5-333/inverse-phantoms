package com.example.mixin;

import com.example.PhantomDazeGoal;
import com.example.PhantomEnragedGoal;
import com.example.SmokeDazeable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.block.CampfireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Phantom.class)
public abstract class PhantomEntityMixin implements SmokeDazeable {

    @Shadow protected GoalSelector goalSelector;

    @Unique private int dazeTicks = 0;
    @Unique private int smokeImmunityTicks = 0;
    @Unique private BlockPos dazeCenter = null;
    
    @Unique private int enrageApproachTicks = 0;
    @Unique private int enrageFrenzyTicks = 0;

    @Override public int getDazeTicks() { return this.dazeTicks; }
    @Override public void setDazeTicks(int ticks) { this.dazeTicks = ticks; }
    @Override public BlockPos getDazeCenter() { return this.dazeCenter; }
    
    @Override public int getEnrageApproachTicks() { return this.enrageApproachTicks; }
    @Override public void setEnrageApproachTicks(int ticks) { this.enrageApproachTicks = ticks; }
    @Override public int getEnrageFrenzyTicks() { return this.enrageFrenzyTicks; }
    @Override public void setEnrageFrenzyTicks(int ticks) { this.enrageFrenzyTicks = ticks; }

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void protectFromSunlight(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        Phantom phantom = (Phantom) (Object) this;
        if (!phantom.level().isClientSide() && phantom.isAlive()) {
            if (this.smokeImmunityTicks > 0) {
                this.smokeImmunityTicks--;
            }
            
            if (this.dazeTicks > 0) {
                this.dazeTicks--;
                if (this.dazeTicks == 0) {
                    // Daze ended. Begin the 20-second window to hit the player.
                    this.enrageApproachTicks = 400; // 20 seconds
                }
            } else if (this.enrageFrenzyTicks > 0) {
                this.enrageFrenzyTicks--;
            } else if (this.enrageApproachTicks > 0) {
                this.enrageApproachTicks--;
            }

            if (this.dazeTicks == 0 && this.enrageFrenzyTicks == 0 && this.enrageApproachTicks == 0 && this.smokeImmunityTicks == 0) {
                if (phantom.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    if (serverLevel.getGameRules().get(com.example.InvertedPhantomsMod.PHANTOM_BEHAVIOR_TWEAKS)) {
                        if (CampfireBlock.isSmokeyPos(phantom.level(), phantom.blockPosition())) {
                            this.dazeTicks = 60; // 3 seconds
                            this.smokeImmunityTicks = 400; // 20 seconds of immunity
                            this.dazeCenter = phantom.blockPosition();
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void onRegisterGoals(CallbackInfo ci) {
        Phantom phantom = (Phantom) (Object) this;
        this.goalSelector.addGoal(0, new PhantomDazeGoal(phantom));
        this.goalSelector.addGoal(1, new PhantomEnragedGoal(phantom));
    }
}
