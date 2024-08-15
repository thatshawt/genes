package xyz.davidpineiro.genes.core.ml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ANN {

    List<Perceptron> inputNeurons = new ArrayList<>();
    List<Perceptron> hiddenNeurons = new ArrayList<>();
    List<Perceptron> outputNeurons = new ArrayList<>();
    WeightMatrix weightMatrix = new WeightMatrix();

    public static class PerceptronCountThing{
        public int count = 0;
        public int maxCount;
        public float lastActivation;
        public float sum = 0.0f;

        public PerceptronCountThing(int maxCount) {
            this.maxCount = maxCount;
        }

        @Override
        public String toString() {
            return "PerceptronCountThing{" +
//                    ", input=" + input +
                    ", count=" + count +
                    ", maxCount=" + maxCount +
                    ", lastActivation=" + lastActivation +
                    ", sum=" + sum +
                    '}';
        }
    }
    private final Map<Perceptron, PerceptronCountThing> neuronMapThing = new HashMap<>();
    Map<Perceptron, Float> outputMap = new HashMap<>();

    private void initNeuronThing(Perceptron perceptron){
        if(!neuronMapThing.containsKey(perceptron)){
            final int maxCount = weightMatrix.connectingTo(perceptron).size();
            neuronMapThing.put(perceptron, new PerceptronCountThing(maxCount));
        }
    }

    private void propagateFrom(Perceptron from){
        final List<Perceptron> connectingTo = weightMatrix.connectionsFrom(from);
        final PerceptronCountThing fromData = neuronMapThing.get(from);
        for(Perceptron to : connectingTo){
            initNeuronThing(to);
            final float weight = weightMatrix.getConnectionData(from, to).weight;

            PerceptronCountThing toData = neuronMapThing.get(to);
            toData.count++;
            toData.sum += fromData.lastActivation * weight;

            if(toData.count == toData.maxCount){
                toData.lastActivation = to.activationFunction(toData.sum);

                if(outputNeurons.contains(to))
                    outputMap.put(to, toData.lastActivation);

                propagateFrom(to);
            }
        }
    }

    public void forwardPass(float[] inputs) {
        for (int i = 0; i < inputs.length; i++) {
            final Perceptron inputNeuron = inputNeurons.get(i);
            final float activation = inputNeuron.activationFunction(inputs[i]);

            PerceptronCountThing data = new PerceptronCountThing(0);
            data.lastActivation = activation;

            neuronMapThing.put(inputNeuron, data);

            propagateFrom(inputNeuron);
        }
    }


    public static void main(String[] args) {
        ANN ann = new ANN();
        Perceptron inputNeuron = new LReLUPerceptron(1.0f);
        Perceptron hiddenNeuron = new LReLUPerceptron(1.0f);
        Perceptron outputNeuron = new LReLUPerceptron(1.0f);

        ann.inputNeurons.add(inputNeuron);
        ann.hiddenNeurons.add(hiddenNeuron);
        ann.outputNeurons.add(outputNeuron);

        WeightMatrix.PerceptronConnectionData data =
                new WeightMatrix.PerceptronConnectionData(0.5f, true);

        ann.weightMatrix.forwardConnect(inputNeuron, hiddenNeuron, data.clone());
        ann.weightMatrix.forwardConnect(hiddenNeuron, outputNeuron, data.clone());

        float[] inputs = {5.0f};
        ann.forwardPass(inputs);

        System.out.println(ann.neuronMapThing);

        float output = ann.outputMap.get(outputNeuron);

        System.out.printf("input: %f, output: %f\n", inputs[0], output);
    }

}
