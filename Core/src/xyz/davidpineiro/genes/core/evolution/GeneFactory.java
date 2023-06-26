package xyz.davidpineiro.genes.core.evolution;

public interface GeneFactory<E extends Gene>{
    E randomGene();
}
