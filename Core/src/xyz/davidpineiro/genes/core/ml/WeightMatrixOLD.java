package xyz.davidpineiro.genes.core.ml;

import java.util.*;

public class WeightMatrixOLD {

    LinkedList<Perceptron> neurons = new LinkedList<>();
    LinkedList<LinkedList<Float>> matrix = new LinkedList<>();

    /**
     * returns all perceptrons that have forward connections to this perceptron
     * @param perceptron
     * @return
     */
    public List<Perceptron> connectingTo(Perceptron perceptron){
        List<Perceptron> perceptrons = new ArrayList<>();
        final int i1 = neurons.indexOf(perceptron);
        for(int i=0;i<matrix.size();i++){
            final Perceptron fromPerceptron = neurons.get(i);
            final boolean connected = matrix.get(i).get(i1) != 0.0f;

            if(connected)perceptrons.add(fromPerceptron);
        }
        return perceptrons;
    }

    /**
     * returns all forward connections from perceptron
     * @param perceptron
     * @return
     */
    public List<Perceptron> connectionsFrom(Perceptron perceptron){
        List<Perceptron> perceptrons = new ArrayList<>();
        final int index = neurons.indexOf(perceptron);
        for(int i=0;i<matrix.get(index).size();i++){
            final Perceptron toPerceptron = neurons.get(i);
            final boolean connected = matrix.get(index).get(i) != 0.0f;

            if(connected)perceptrons.add(toPerceptron);
        }
        return perceptrons;
    }

    private void initNeuron(Perceptron perceptron1){
        if(!neurons.contains(perceptron1)){//init perceptron1 if not already
            neurons.push(perceptron1);
            //we do -1 cus we add 1 to every row in the bottom for loop so yea
            matrix.push(new LinkedList<>(Collections.nCopies(neurons.size()-1, 0.0f)));

            for(int i=0;i<matrix.size();i++){
                matrix.get(i).push(0.0f);//we need to add one to every row to keep it a square
            }
        }
    }

    /**
     * connecttss perceptron1 forward to perceptron1
     * @param perceptron1
     * @param perceptron2
     */
    public void forwardConnect(Perceptron perceptron1, Perceptron perceptron2, float weight){
        initNeuron(perceptron1);
        initNeuron(perceptron2);

        final int i1 = neurons.indexOf(perceptron1);
        final int i2 = neurons.indexOf(perceptron2);

        matrix.get(i1).set(i2, weight);
    }

    private void pruneNeuron(Perceptron perceptron){
        final int i1 = neurons.indexOf(perceptron);
        //check forward connections from perceptron
        boolean hasOneConection = false;
        for(float weight : matrix.get(i1)){
            hasOneConection = hasOneConection || (weight != 0.0f);
        }
        if(!hasOneConection){
            //check forward connections TO perceptron
            for(int i=0;i<matrix.size();i++){
                float weight = matrix.get(i).get(i1);
                hasOneConection = hasOneConection || (weight != 0.0f);
            }
            if(!hasOneConection){
                //remove the neuron from matrix
                removeNeuron(perceptron);
            }
        }
    }
    private void removeNeuron(Perceptron perceptron){
        final int i1 = neurons.indexOf(perceptron);
        for(int i = 0;i<matrix.size();i++){
            matrix.get(i).remove(i1);
        }
        matrix.remove(i1);
        neurons.remove(i1);
    }

    /**
     * deletes forward connection from perceptron1 to perceptron2
     * @param perceptron1
     * @param perceptron2
     */
    public void deleteConnection(Perceptron perceptron1, Perceptron perceptron2) {
        //assume that the neurons exist in the matrix
        //mark the connection as false(based off previous assumption)

        final int i1 = neurons.indexOf(perceptron1);
        final int i2 = neurons.indexOf(perceptron2);

        //set them to be false so they arent connected
        matrix.get(i1).set(i2, 0.0f);

        //remove the neurons if they no longer have any connections for space reasons
        pruneNeuron(perceptron1);
        pruneNeuron(perceptron2);
    }

}
