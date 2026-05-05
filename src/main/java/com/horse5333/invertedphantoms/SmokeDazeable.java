package com.horse5333.invertedphantoms;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface SmokeDazeable {
    int getDazeTicks();
    void setDazeTicks(int ticks);
    BlockPos getDazeCenter();
    int getEnrageApproachTicks();
    void setEnrageApproachTicks(int ticks);
    int getEnrageFrenzyTicks();
    void setEnrageFrenzyTicks(int ticks);

    // Group bravery / noise-scared state
    int getScaredTicks();
    void setScaredTicks(int ticks);
    Vec3 getScareSourcePos();
    void setScareSourcePos(Vec3 pos);
}
