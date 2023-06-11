package io.github.stuff_stuffs.train_lib.mixin;

import net.minecraft.util.math.AxisTransformation;
import org.joml.Matrix3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AxisTransformation.class)
public abstract class MixinAxisTransformation {
    @Shadow
    @Final
    private Matrix3f matrix;

    @Shadow
    public abstract int map(int oldAxis);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void clear(final String string, final int i, final int xMapping, final int yMapping, final int zMapping, final CallbackInfo ci) {
        matrix.zero();
        matrix.set(map(0), 0, 1.0F);
        matrix.set(map(1), 1, 1.0F);
        matrix.set(map(2), 2, 1.0F);
    }
}
