package io.github.stuff_stuffs.train_lib.internal.common;

import io.github.stuff_stuffs.train_lib.api.common.cart.Cart;
import io.github.stuff_stuffs.train_lib.api.common.cart.CartTypes;
import io.github.stuff_stuffs.train_lib.api.common.cart.TrainLibApi;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.CargoType;
import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.EntityCargo;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRail;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.MinecartRailProvider;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.basic.DelegatingMinecartRail;
import io.github.stuff_stuffs.train_lib.api.common.cart.mine.basic.DelegatingMinecartRailProvider;
import io.github.stuff_stuffs.train_lib.internal.common.config.TrainLibConfig;
import io.github.stuff_stuffs.train_lib.internal.common.entity.TrainLibEntities;
import io.github.stuff_stuffs.train_lib.internal.common.item.TrainLibItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrainLib implements ModInitializer {
    public static final String MOD_ID = "train_lib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final TrainLibConfig CONFIG = TrainLibConfig.createAndLoad();

    @Override
    public void onInitialize() {
        TrainLibEntities.init();
        CargoType.init();
        TrainLibItems.init();
        CartTypes.init();
        TrainLibApi.MINECART_RAIL_BLOCK_API.registerFallback((world, pos, state, blockEntity, context) -> {
            if (state.getBlock() instanceof AbstractRailBlock railBlock) {
                final Property<RailShape> property = railBlock.getShapeProperty();
                final RailShape shape = state.get(property);
                if (railBlock instanceof PoweredRailBlock powered) {
                    if (state.get(PoweredRailBlock.POWERED)) {
                        return MinecartRailAdaptor.poweredShapeBased(shape, pos);
                    } else {
                        return MinecartRailAdaptor.unpoweredShapeBased(shape, pos);
                    }
                }
                MinecartRailProvider provider = MinecartRailAdaptor.shapeBased(shape, pos);
                if (railBlock == Blocks.ACTIVATOR_RAIL) {
                    if (state.get(PoweredRailBlock.POWERED)) {
                        provider = new DelegatingMinecartRailProvider(provider) {
                            @Override
                            protected MinecartRail wrap(final MinecartRail rail) {
                                return new DelegatingMinecartRail(rail) {
                                    @Override
                                    public void onRail(final Cart minecart, final double startProgress, final double endProgress, final double time) {
                                        super.onRail(minecart, startProgress, endProgress, time);
                                        if (minecart.cargo() instanceof EntityCargo) {
                                            minecart.cargo(null);
                                        }
                                    }
                                };
                            }
                        };
                    }
                }
                return provider;
            }
            return null;
        });
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }
}
