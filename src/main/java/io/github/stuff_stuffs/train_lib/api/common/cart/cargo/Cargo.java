package io.github.stuff_stuffs.train_lib.api.common.cart.cargo;

import com.mojang.serialization.Codec;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;

import java.util.List;

public interface Cargo {
    Codec<Cargo> CODEC = CargoType.REGISTRY.getCodec().dispatchStable(Cargo::type, CargoType::codec);

    CargoType<?> type();

    double mass();

    List<ItemStack> drops(DamageSource source);
}
