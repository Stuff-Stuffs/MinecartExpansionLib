package io.github.stuff_stuffs.train_lib.api.common.cart.cargo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.train_lib.api.common.cart.Minecart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
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

    @Override
    public List<ItemStack> drops(final DamageSource source) {
        return List.of();
    }

    @Override
    public void tick(final Minecart minecart) {
        final List<Entity> passengerList = new ArrayList<>(minecart.holder().getPassengerList());
        boolean main = false;
        for (final Entity entity : passengerList) {
            if (!entity.getUuid().equals(id)) {
                entity.stopRiding();
            } else {
                main = true;
            }
        }
        if (!main) {
            minecart.cargo(null);
        }
    }
}
