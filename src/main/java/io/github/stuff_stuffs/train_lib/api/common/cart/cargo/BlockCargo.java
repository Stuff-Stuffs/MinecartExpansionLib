package io.github.stuff_stuffs.train_lib.api.common.cart.cargo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;

public record BlockCargo(BlockState blockState, Optional<NbtCompound> nbt) implements Cargo {
    public static final Codec<BlockCargo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("blockState").forGetter(BlockCargo::blockState),
            NbtCompound.CODEC.optionalFieldOf("nbt").forGetter(BlockCargo::nbt)
    ).apply(instance, BlockCargo::new));

    @Override
    public CargoType<?> type() {
        return CargoType.BLOCK_CARGO_TYPE;
    }

    @Override
    public double mass() {
        return 1.0;
    }
}
