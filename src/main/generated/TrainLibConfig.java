package io.github.stuff_stuffs.train_lib.internal.common.config;

import blue.endless.jankson.Jankson;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.util.Observable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TrainLibConfig extends ConfigWrapper<io.github.stuff_stuffs.train_lib.internal.common.config.TrainLibConfigModel> {

    private final Option<java.lang.Integer> maxRecursion = this.optionForKey(new Option.Key("maxRecursion"));
    private final Option<java.lang.Integer> maxSpeed = this.optionForKey(new Option.Key("maxSpeed"));
    private final Option<java.lang.Integer> maxTrainSize = this.optionForKey(new Option.Key("maxTrainSize"));
    private final Option<java.lang.Double> gravity = this.optionForKey(new Option.Key("gravity"));
    private final Option<java.lang.Integer> maxPathfindingLimit = this.optionForKey(new Option.Key("maxPathfindingLimit"));

    private TrainLibConfig() {
        super(io.github.stuff_stuffs.train_lib.internal.common.config.TrainLibConfigModel.class);
    }

    private TrainLibConfig(Consumer<Jankson.Builder> janksonBuilder) {
        super(io.github.stuff_stuffs.train_lib.internal.common.config.TrainLibConfigModel.class, janksonBuilder);
    }

    public static TrainLibConfig createAndLoad() {
        var wrapper = new TrainLibConfig();
        wrapper.load();
        return wrapper;
    }

    public static TrainLibConfig createAndLoad(Consumer<Jankson.Builder> janksonBuilder) {
        var wrapper = new TrainLibConfig(janksonBuilder);
        wrapper.load();
        return wrapper;
    }

    public int maxRecursion() {
        return maxRecursion.value();
    }

    public void maxRecursion(int value) {
        maxRecursion.set(value);
    }

    public int maxSpeed() {
        return maxSpeed.value();
    }

    public void maxSpeed(int value) {
        maxSpeed.set(value);
    }

    public int maxTrainSize() {
        return maxTrainSize.value();
    }

    public void maxTrainSize(int value) {
        maxTrainSize.set(value);
    }

    public double gravity() {
        return gravity.value();
    }

    public void gravity(double value) {
        gravity.set(value);
    }

    public int maxPathfindingLimit() {
        return maxPathfindingLimit.value();
    }

    public void maxPathfindingLimit(int value) {
        maxPathfindingLimit.set(value);
    }




}

