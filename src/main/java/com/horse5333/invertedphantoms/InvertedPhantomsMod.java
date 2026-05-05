package com.horse5333.invertedphantoms;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.resources.Identifier;

public class InvertedPhantomsMod implements ModInitializer {
    public static GameRule<Boolean> PHANTOM_BEHAVIOR_TWEAKS;

    @Override
    public void onInitialize() {
        PHANTOM_BEHAVIOR_TWEAKS = GameRuleBuilder.forBoolean(true)
            .category(GameRuleCategory.MOBS)
            .buildAndRegister(Identifier.fromNamespaceAndPath("inverted_phantoms", "phantom_behavior_tweaks"));
    }
}
