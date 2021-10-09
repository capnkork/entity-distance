package com.capnkork.entitydistance.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.EntityType;

import com.capnkork.entitydistance.config.EntityDistanceConfig;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ItemFrameEntity.class)
public abstract class MixinItemFrameEntity {
    private Integer cachedDistanceConfig = null;
    private int cachedConfigVersionId = 0;

    @ModifyConstant(
            constant = @Constant(doubleValue = 64.0D, ordinal = 0),
            method = "shouldRender(D)Z"
    )
    private double applyRenderDistance(double distance) {
        EntityDistanceConfig config = EntityDistanceConfig.getInstance();
        if (cachedDistanceConfig == null || cachedConfigVersionId != config.getVersionId()) {
            cachedDistanceConfig = config.getEntityDistanceByType(((Entity) (Object) this).getType());
            cachedConfigVersionId = config.getVersionId();
            if (cachedDistanceConfig == null) {
                System.err.printf(
                        "[Entity Distance Mod] Unable to get entity distance config for entity %s, using default value\n",
                        EntityType.getId(((Entity) (Object) this).getType()).toString()
                );
                cachedDistanceConfig = 64;
            }
        }

        return (double) cachedDistanceConfig;
    }
}
