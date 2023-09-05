package xyz.davidpineiro.genes.core.ml;

public class LReLUPerceptron extends Perceptron{
    public LReLUPerceptron(float bias) {
        super(bias);
    }

    @Override
    protected float activationFunction(float input) {
        return (float) Math.max(0.1*input, input);
    }
}
