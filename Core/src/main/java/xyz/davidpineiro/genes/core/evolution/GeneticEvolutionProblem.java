package xyz.davidpineiro.genes.core.evolution;

public abstract class GeneticEvolutionProblem<G extends IGene> {

    public abstract float fitness(Genome<G> genome);
    public abstract boolean satisfies(float fitness, Genome<G> genome);

}
