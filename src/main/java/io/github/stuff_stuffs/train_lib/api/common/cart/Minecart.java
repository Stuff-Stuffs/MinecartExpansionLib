package io.github.stuff_stuffs.train_lib.api.common.cart;

import io.github.stuff_stuffs.train_lib.api.common.cart.cargo.Cargo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public interface Minecart extends MinecartView {
    void speed(double speed);

    void progress(double progress);

    void cargo(Cargo cargo);

    void addSpeed(double speed);

    void position(Vec3d position);

    Entity holder();

    @Override
    @Nullable Minecart attached();

    @Override
    @Nullable Minecart attachment();

    interface Tracker {
        void onMove(Vec3d position, Vec3d tangent, Vec3d up, double time);

        void reset();

        void onCargoChange();

        void onAttachedChange();
    }

    interface OffRailHandler {
        double handle(Minecart minecart, @Nullable Minecart following, Vec3d position, double time);

        boolean shouldDisconnect(Minecart minecart, @Nullable Minecart following);
    }
}
