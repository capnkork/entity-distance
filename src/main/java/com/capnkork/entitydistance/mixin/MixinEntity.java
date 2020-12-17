package com.capnkork.entitydistance.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;

import com.capnkork.entitydistance.config.EntityDistanceConfig;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Invoker("getRenderDistanceMultiplier")
    public abstract double invokeGetRenderDistanceMultiplier();

    @Invoker("getBoundingBox")
    public abstract Box invokeGetBoundingBox();

    @Invoker("getType")
    public abstract EntityType<?> invokeGetType();

    private Integer cachedDistanceConfig = null;
    private int cachedConfigVersionId = 0;

    @Overwrite
    public boolean shouldRender(double distance) {
        double d = invokeGetBoundingBox().getAverageSideLength();
        if (Double.isNaN(d)) {
            d = 1.0D;
        }

        EntityDistanceConfig config = EntityDistanceConfig.getInstance();
        if (cachedDistanceConfig == null || cachedConfigVersionId != config.getVersionId()) {
            cachedDistanceConfig = config.getEntityDistanceByType(invokeGetType());
            cachedConfigVersionId = config.getVersionId();
            if (cachedDistanceConfig == null) {
                System.err.printf("Entity Distance Mod: Unable to get entity distance config for entity %s, using default value\n", EntityType.getId(invokeGetType()).toString());
                cachedDistanceConfig = 64;
            }
        }

        d *= ((double) cachedDistanceConfig) * invokeGetRenderDistanceMultiplier();
        return distance < d * d;
    }
}
