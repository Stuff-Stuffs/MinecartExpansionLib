package io.github.stuff_stuffs.train_lib.internal.common.item;

import io.github.stuff_stuffs.train_lib.internal.common.entity.FastMinecartEntity;
import io.github.stuff_stuffs.train_lib.internal.common.entity.TrainLibEntities;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class FastMinecartItem extends Item {
    public FastMinecartItem() {
        super(new FabricItemSettings().maxCount(4));
    }

    @Override
    public ActionResult useOnBlock(final ItemUsageContext context) {
        final World world = context.getWorld();
        final BlockPos blockPos = context.getBlockPos();
        final BlockState blockState = world.getBlockState(blockPos);
        if (!blockState.isIn(BlockTags.RAILS)) {
            return ActionResult.FAIL;
        } else {
            final ItemStack itemStack = context.getStack();
            if (!world.isClient) {
                final FastMinecartEntity entity = new FastMinecartEntity(TrainLibEntities.FAST_MINECART_ENTITY_TYPE, world);
                entity.setPosition(context.getHitPos().add(0, 0.4, 0));
                if (itemStack.hasCustomName()) {
                    entity.setCustomName(itemStack.getName());
                }

                world.spawnEntity(entity);
                world.emitGameEvent(GameEvent.ENTITY_PLACE, blockPos, GameEvent.Emitter.of(context.getPlayer(), world.getBlockState(blockPos.down())));
            }

            itemStack.decrement(1);
            return ActionResult.success(world.isClient);
        }
    }
}
