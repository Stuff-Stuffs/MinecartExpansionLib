package io.github.stuff_stuffs.train_lib.internal.common;

import io.github.stuff_stuffs.train_lib.api.common.cart.MinecartAPI;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.CargoType;
import io.github.stuff_stuffs.train_lib.internal.common.entity.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrainLib implements ModInitializer {
    public static final String MOD_ID = "train_lib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.init();
        CargoType.init();
        MinecartAPI.MINECART_RAIL_BLOCK_API.registerFallback((world, pos, state, blockEntity, context) -> {
            if (state.getBlock() instanceof AbstractRailBlock railBlock) {
                final Property<RailShape> property = railBlock.getShapeProperty();
                final RailShape shape = state.get(property);
                if (railBlock instanceof PoweredRailBlock powered && state.get(PoweredRailBlock.POWERED)) {
                    return MinecartRailAdaptor.poweredShapeBased(shape, pos);
                }
                return MinecartRailAdaptor.shapeBased(shape, pos);
            }
            return null;
        });
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }
}
