package xyz.davidpineiro.genes.core.ml;

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

    public static class PerceptronConnectionData extends BaseConnectionData implements Cloneable {
        public float weight = 1.0f;

        public PerceptronConnectionData() {
        }

        public PerceptronConnectionData(float weight, boolean connected) {
            this.weight = weight;
            this.connected = connected;
        }

        @Override
        public PerceptronConnectionData clone() {
            PerceptronConnectionData data = new PerceptronConnectionData();
            data.connected = this.connected;
            data.weight = this.weight;
            return data;
        }

//        public float lastActivation = 0.0f;
//        public int count = 0;
//        public List<InputWeightTuple> weightStack = new ArrayList<>();
    }

}
