package xyz.davidpineiro.genes.core.evolution;

public interface Gene extends Cloneable {

    boolean isActive();
    void mutateGene(float geneMutateChance, float activeMutateChance);
    Gene clone();

}