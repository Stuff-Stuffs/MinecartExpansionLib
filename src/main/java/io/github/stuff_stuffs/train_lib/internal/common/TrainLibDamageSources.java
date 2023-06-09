package io.github.stuff_stuffs.train_lib.internal.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class TrainLibDamageSources {
    public static final RegistryKey<DamageType> TRAIN_DAMAGE_TYPE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, TrainLib.id("train"));

    public static DamageSource createTrain(final Entity train) {
        return new DamageSource(train.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(TrainLibDamageSources.TRAIN_DAMAGE_TYPE), train);
    }

    private TrainLibDamageSources() {
    }
}
