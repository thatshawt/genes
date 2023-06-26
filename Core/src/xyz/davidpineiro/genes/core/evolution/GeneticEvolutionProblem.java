package xyz.davidpineiro.genes.core.evolution;

public abstract class GeneticEvolutionProblem<E extends Gene> {

    protected abstract float fitness(Genome<E> genome);
    protected abstract boolean satisfies(float fitness, Genome<E> genome);

}
