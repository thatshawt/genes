package xyz.davidpineiro.genes.core.evolution;

import java.util.ArrayList;

public abstract class Genome<E extends Gene> extends ArrayList<E> {

    abstract Genome<E> crossover(Genome<E> other);

}
