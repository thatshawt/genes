package xyz.davidpineiro.genes.core.evolution;

public interface GenomeFactory<E extends Gene>{


    Genome<E> randomGenome();
}
