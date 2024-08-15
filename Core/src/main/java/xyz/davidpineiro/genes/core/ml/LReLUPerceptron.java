package xyz.davidpineiro.genes.core.ml;

public class LReLUPerceptron extends Perceptron{
    public LReLUPerceptron(float bias) {
        super(bias);
    }

    @Override
    protected float activationFunction(float input) {
        float value = (float) Math.max(0.1*input, input);
//        System.out.printf("activvate:%f\n", value);
        return value;
    }

    @Override
    public String toString() {
        return "LReLUPerceptron{" +
                "bias=" + bias +
                '}';
    }
}
