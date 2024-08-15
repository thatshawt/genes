package xyz.davidpineiro.genes.core.problems;

import xyz.davidpineiro.genes.core.evolution.GeneticEvolutionProblem;
import xyz.davidpineiro.genes.core.evolution.Genome;
import xyz.davidpineiro.genes.core.evolution.IGene;

import java.util.HashMap;
import java.util.Map;

public class CompoundProblem<G extends IGene> extends GeneticEvolutionProblem<G> {

    final GeneticEvolutionProblem<G> problems[];
    final Map<GeneticEvolutionProblem<G>, Float> fitnessMap = new HashMap<>();

    //idk why but intellij told me to add @SafeVarargs
    @SafeVarargs
    public CompoundProblem(GeneticEvolutionProblem<G>... problems) {
        this.problems = problems;
    }

    @Override
    public float fitness(Genome<G> genome) {
        float sum = 0.0f;
        for(GeneticEvolutionProblem<G> problem : problems){
            final float fitness = problem.fitness(genome);
            sum += fitness;
            fitnessMap.put(problem, fitness);
        }
        return sum;
    }

    @Override
    public boolean satisfies(float fitnessZ, Genome<G> genome) {
        boolean sum = true;
        for(GeneticEvolutionProblem<G> problem : problems){
            final float fitness = fitnessMap.get(problem);
            sum = sum && problem.satisfies(fitness, genome);
        }
        return sum;
    }

}
