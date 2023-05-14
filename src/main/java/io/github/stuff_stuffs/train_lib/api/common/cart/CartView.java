package io.github.stuff_stuffs.train_lib.api.common.cart;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public interface CartView extends CartDataView {
    Vec3d velocity();

    @Override
    @Nullable CartView attached();

    @Override
    @Nullable CartView attachment();

    default double massAhead() {
        double m = 0;
        CartView view = this.attached();
        while (view != null) {
            m = m + view.mass();
            view = view.attached();
        }
        return m;
    }

    default double massBehind() {
        double m = 0;
        CartView view = this.attachment();
        while (view != null) {
            m = m + view.mass();
            view = view.attachment();
        }
        return m;
    }

    default double trainMass() {
        return massAhead() + massBehind() + mass();
    }
}
