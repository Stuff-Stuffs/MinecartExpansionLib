package io.github.stuff_stuffs.train_lib.api.common.cart;

import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public interface Cart extends CartView {
    void speed(double speed);

    void progress(double progress);

    void cargo(Cargo cargo);

    void addSpeed(double speed);

    void position(Vec3d position);

    Entity holder();

    @Override
    @Nullable Cart attached();

    @Override
    @Nullable Cart attachment();

    interface Tracker {
        void onMove(Vec3d position, Vec3d tangent, Vec3d up, double time);

        void reset();

        void onCargoChange();

        void trainChange();
    }

    interface OffRailHandler {
        double handle(Cart minecart, @Nullable Cart following, Vec3d position, double time);

        boolean shouldDisconnect(Cart minecart, Cart following);
    }
}
