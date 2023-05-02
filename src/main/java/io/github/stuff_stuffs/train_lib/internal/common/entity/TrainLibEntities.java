package io.github.stuff_stuffs.train_lib.internal.common.entity;

import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class TrainLibEntities {
    public static final EntityType<FastMinecartEntity> FAST_MINECART_ENTITY_TYPE = FabricEntityTypeBuilder.<FastMinecartEntity>create().spawnGroup(SpawnGroup.MISC).entityFactory(FastMinecartEntity::new).dimensions(EntityDimensions.fixed(1, 1)).build();

    public static void init() {
        Registry.register(Registries.ENTITY_TYPE, TrainLib.id("fast_minecart"), FAST_MINECART_ENTITY_TYPE);
    }

    private TrainLibEntities() {
    }
}
