package xyz.davidpineiro.genes.core.ml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ANN {

    List<Perceptron> inputNeurons;
    List<Perceptron> hiddenNeurons;
    List<Perceptron> outputNeurons;

    WeightMatrix weightMatrix = new WeightMatrix();

    private static class PerceptronCountThing{
        public float input;
        public int count;
        public final int maxCount;
        public float lastActivation;
        public float sum = 0.0f;

        public PerceptronCountThing(int maxCount) {
            this.maxCount = maxCount;
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
            PerceptronCountThing toData = neuronMapThing.get(to);
            final float weight = weightMatrix.getConnectionData(from, to).weight;

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
        System.out.println("sussssss");
    }

}
