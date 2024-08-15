package xyz.davidpineiro.genes.core.ml;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public abstract class Perceptron {

    float bias;
    public final UUID uuid = UUID.randomUUID();

    public Perceptron(float bias) {
        this.bias = bias;
    }

    public float activation(float[] inputs, float[] weights){
        float counter = 0.0f;
        for(int i=0;i<inputs.length;i++){
            final float weight = weights[i];
            final float input = inputs[i];
            counter += weight*input;
        }
        return activationFunction(counter + bias);
    }

    protected abstract float activationFunction(float input);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Perceptron that = (Perceptron) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
