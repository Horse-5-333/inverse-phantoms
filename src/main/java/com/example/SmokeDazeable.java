package com.example;

import net.minecraft.core.BlockPos;

public interface SmokeDazeable {
    int getDazeTicks();
    void setDazeTicks(int ticks);
    BlockPos getDazeCenter();
    int getEnrageApproachTicks();
    void setEnrageApproachTicks(int ticks);
    int getEnrageFrenzyTicks();
    void setEnrageFrenzyTicks(int ticks);
}
