package xyz.davidpineiro.genes.core.evolution;

public interface GeneFactory<E extends IGene>{
    E randomGene();
}
