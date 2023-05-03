package io.github.stuff_stuffs.train_lib.api.common.cart.cargo;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.train_lib.api.common.cart.Minecart;
import io.github.stuff_stuffs.train_lib.api.common.cart.MinecartRail;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public interface Cargo {
    Codec<Cargo> CODEC = CargoType.REGISTRY.getCodec().dispatchStable(Cargo::type, CargoType::codec);

    CargoType<?> type();

    double mass();

    List<ItemStack> drops(DamageSource source);

    default void onRail(final BlockPos pos, final MinecartRail rail, final Minecart minecart, final double time) {
    }

    default void tick(final Minecart minecart) {
    }
}
