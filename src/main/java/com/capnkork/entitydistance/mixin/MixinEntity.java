package com.capnkork.entitydistance.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import com.capnkork.entitydistance.config.EntityDistanceConfig;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Invoker("getType")
    public abstract EntityType<?> invokeGetType();

    private Integer cachedDistanceConfig = null;
    private int cachedConfigVersionId = 0;

    @ModifyConstant(
        constant = @Constant(doubleValue = 64.0D, ordinal = 0),
        method = "shouldRender(D)Z"
    )
    private double applyRenderDistance(double distance) {
        EntityDistanceConfig config = EntityDistanceConfig.getInstance();
        if (cachedDistanceConfig == null || cachedConfigVersionId != config.getVersionId()) {
            cachedDistanceConfig = config.getEntityDistanceByType(invokeGetType());
            cachedConfigVersionId = config.getVersionId();
            if (cachedDistanceConfig == null) {
                System.err.printf(
                    "[Entity Distance Mod] Unable to get entity distance config for entity %s, using default value\n",
                    EntityType.getId(invokeGetType()).toString()
                );
                cachedDistanceConfig = 64;
            }
        }

        return (double) cachedDistanceConfig;
    }
}
