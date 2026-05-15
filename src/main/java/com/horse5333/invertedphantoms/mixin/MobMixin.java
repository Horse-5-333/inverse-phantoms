package com.horse5333.invertedphantoms.mixin;

import com.horse5333.invertedphantoms.InvertedPhantomsMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public class MobMixin {

    @Inject(method = "burnUndead", at = @At("HEAD"), cancellable = true)
    private void suppressPhantomDayBurn(CallbackInfo ci) {
        if ((Object) this instanceof Phantom) {
            ci.cancel(); // Prevent burning
        }
    }
}
