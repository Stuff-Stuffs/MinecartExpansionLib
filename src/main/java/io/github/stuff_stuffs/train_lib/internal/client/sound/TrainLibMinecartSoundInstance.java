package io.github.stuff_stuffs.train_lib.internal.client.sound;

import io.github.stuff_stuffs.train_lib.impl.common.MinecartImpl;
import io.github.stuff_stuffs.train_lib.internal.common.TrainLib;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class TrainLibMinecartSoundInstance extends MovingSoundInstance {
    private final MinecartImpl minecart;

    public TrainLibMinecartSoundInstance(MinecartImpl minecart) {
        super(SoundEvents.ENTITY_MINECART_RIDING, SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.minecart = minecart;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.0F;
        this.x = minecart.position().x;
        this.y = minecart.position().y;
        this.z = minecart.position().z;
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public void tick() {
        if(minecart.isDestroyed() || minecart.holder().isRemoved()) {
            setDone();
        } else {
            this.x = minecart.position().x;
            this.y = minecart.position().y;
            this.z = minecart.position().z;
            float f = (float) this.minecart.velocity().length();
            if (f >= 0.01F) {
                double v;
                double p;
                if(f < 1.0) {
                    double x1 = 1 - f;
                    double x1_2 = x1 * x1;
                    v = 1-x1_2;
                    p = (1-x1_2) *0.5 + 0.5;
                } else {
                    v = 1.0;
                    p = 1.0;
                }
                this.volume = (float)Math.min(v, TrainLib.CONFIG.maxMinecartVolume());
                this.pitch = (float)p;
            } else {
                this.volume = 0.0F;
            }
        }
    }
}
