package xyz.davidpineiro.genes.core.ml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class ConnectionMatrix<N, D extends ConnectionMatrix.IConnectionData> {

    private final ConnectionDataFactory<D> dataFactory;
    public ConnectionMatrix(ConnectionDataFactory<D> dataFactory){
        this.dataFactory = dataFactory;
    }

    public static class BaseConnectionData implements IConnectionData{
        public boolean connected;
        @Override
        public boolean isConnected() {
            return this.connected;
        }

        @Override
        public void disconnect() {
            this.connected = false;
        }

        @Override
        public void connect() {
            this.connected = true;
        }

        @Override
        public BaseConnectionData clone(){
            BaseConnectionData data = new BaseConnectionData();
            data.connected = this.connected;
            return data;
        }
    }

    public interface IConnectionData {
        boolean isConnected();
        void disconnect();
        void connect();
    }

    public interface ConnectionDataFactory<D>{
        D getEmptyConnectionData();
    }

    private LinkedList<N> nodes = new LinkedList<>();
    private LinkedList<LinkedList<D>> matrix = new LinkedList<>();

    public D getConnectionData(N node1, N node2){
        final int i1 = nodes.indexOf(node1);
        final int i2 = nodes.indexOf(node2);

        return matrix.get(i1).get(i2);
    }

    /**
     * returns all members that have forward connections to this member
     * @param node
     * @return
     */
    public List<N> connectingTo(N node){
        List<N> result = new ArrayList<>();
        final int i1 = nodes.indexOf(node);
        for(int i=0;i<matrix.size();i++){
            final N fromNode = nodes.get(i);
//            final boolean connected = matrix.get(i).get(i1) != 0.0f;
            final boolean connected = matrix.get(i).get(i1).isConnected();

            if(connected)result.add(fromNode);
        }
        return result;
    }

    /**
     * returns all forward connections from perceptron
     * @param node
     * @return
     */
    public List<N> connectionsFrom(N node){
        List<N> result = new ArrayList<>();
        final int index = nodes.indexOf(node);
        for(int i=0;i<matrix.get(index).size();i++){
            final N toPerceptron = nodes.get(i);
            final boolean connected = matrix.get(index).get(i).isConnected();

            if(connected)result.add(toPerceptron);
        }
        return result;
    }

    private void initNode(N node){
        if(!nodes.contains(node)){//init perceptron1 if not already
            nodes.push(node);
            //we do -1 cus we add 1 to every row in the bottom for loop so yea
            matrix.push(new LinkedList<>(
                    Collections.nCopies(nodes.size()-1, dataFactory.getEmptyConnectionData())
            ));

            for(int i=0;i<matrix.size();i++){
                matrix.get(i).push(dataFactory.getEmptyConnectionData());//we need to add one to every row to keep it a square
            }
        }
    }

    /**
     * connecttss perceptron1 forward to perceptron1
     * @param node1
     * @param node2
     */
    public void forwardConnect(N node1, N node2, D data){
        initNode(node1);
        initNode(node2);

        final int i1 = nodes.indexOf(node1);
        final int i2 = nodes.indexOf(node2);

        matrix.get(i1).set(i2, data);
    }

    private void pruneNode(N node){
        final int i1 = nodes.indexOf(node);
        //check forward connections from perceptron
        boolean hasOneConection = false;
        for(D weight : matrix.get(i1)){
            hasOneConection = hasOneConection || weight.isConnected();
        }
        if(!hasOneConection){
            //check forward connections TO perceptron
            for(int i=0;i<matrix.size();i++){
                D weight = matrix.get(i).get(i1);
                hasOneConection = hasOneConection || weight.isConnected();
            }
            if(!hasOneConection){
                //remove the neuron from matrix
                removeNode(node);
            }
        }
    }
    private void removeNode(N node){
        final int i1 = nodes.indexOf(node);
        for(int i = 0;i<matrix.size();i++){
            matrix.get(i).remove(i1);
        }
        matrix.remove(i1);
        nodes.remove(i1);
    }

    /**
     * deletes forward connection from perceptron1 to perceptron2
     * @param node1
     * @param node2
     */
    public void deleteConnection(N node1, N node2) {
        //assume that the neurons exist in the matrix
        //mark the connection as false(based off previous assumption)

        final int i1 = nodes.indexOf(node1);
        final int i2 = nodes.indexOf(node2);

        D data = matrix.get(i1).get(i2);
        data.disconnect();

        //set them to be false so they arent connected
//        matrix.get(i1).set(i2, data);

        //remove the neurons if they no longer have any connections for space reasons
        pruneNode(node1);
        pruneNode(node2);
    }

}
