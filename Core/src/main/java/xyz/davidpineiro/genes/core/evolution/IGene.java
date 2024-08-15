package xyz.davidpineiro.genes.core.evolution;

public interface IGene extends Cloneable {

    boolean isActive();

    void mutate();
    void flipActive();

    IGene clone();

}