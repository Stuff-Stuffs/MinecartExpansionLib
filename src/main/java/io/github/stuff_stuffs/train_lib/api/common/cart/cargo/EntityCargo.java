package io.github.stuff_stuffs.train_lib.api.common.cart.cargo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record EntityCargo(UUID id) implements Cargo {
    public static final Codec<EntityCargo> CODEC = RecordCodecBuilder.create(instance -> instance.group(Uuids.CODEC.fieldOf("id").forGetter(EntityCargo::id)).apply(instance, EntityCargo::new));

    @Override
    public CargoType<?> type() {
        return CargoType.ENTITY_CARGO_TYPE;
    }

    @Override
    public double mass() {
        return 1;
    }
}
