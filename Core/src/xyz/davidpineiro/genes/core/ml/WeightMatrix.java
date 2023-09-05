package xyz.davidpineiro.genes.core.ml;

import java.util.ArrayList;
import java.util.List;

public class WeightMatrix extends ConnectionMatrix<Perceptron, WeightMatrix.PerceptronConnectionData>{

    public WeightMatrix() {
        super(PerceptronConnectionData::new);
    }

    public static class InputWeightTuple{
        public float input;
        public float weight;
        public InputWeightTuple(float input, float weight) {
            this.input = input;
            this.weight = weight;
        }
    }

    public static class PerceptronConnectionData extends ConnectionMatrix.AbstractConnectionData{
        public float weight = 1.0f;
//        public float lastActivation = 0.0f;
//        public int count = 0;
//        public List<InputWeightTuple> weightStack = new ArrayList<>();
    }

}
