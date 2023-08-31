package xyz.davidpineiro.genes.core.evolution;

public abstract class GeneticEvolutionProblem<E extends IGene> {

    protected abstract float fitness(Genome<E> genome);
    protected abstract boolean satisfies(float fitness, Genome<E> genome);

}
