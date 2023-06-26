package xyz.davidpineiro.genes.core.evolution;

import xyz.davidpineiro.genes.core.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class EvolverSolver<E extends Gene> {

    private static class GenomeResult<E extends Gene>{
        public final Genome<E> genome;
        public final float fitness;
        public final boolean satisfies;

        public GenomeResult(Genome<E> genome, float fitness, boolean satisfies) {
            this.genome = genome;
            this.fitness = fitness;
            this.satisfies = satisfies;
        }

        @Override
        public String toString() {
            return "GenomeResult{" +
                    "fitness=" + fitness +
                    ", satisfies=" + satisfies +
                    ", genome=" + genome +
                    '}';
        }
    }

    public Genome<E> solve(GeneticEvolutionProblem<E> problem,
//                                            GeneFactory<E> geneFactory,
                                            GenomeFactory<E> genomeFactory){
        boolean satisfied = false;

        List<Genome<E>> population = new ArrayList<>();

        //make 100 genomes idk
        for(int i=0;i<100;i++){
            population.add(genomeFactory.randomGenome());
        }

        while(!satisfied){
            //get fitness of all our genomes rn
            List<GenomeResult<E>> results = new ArrayList<>();

            // get fitnessesese
            {
                for (final Genome<E> genome : population) {
                    final float fitness = problem.fitness(genome);
                    satisfied = problem.satisfies(fitness, genome);

                    if(satisfied){
                        return genome;
                    }

                    results.add(new GenomeResult<E>(genome, fitness, satisfied));

                }

                //sort by fitness
                results.sort((a, b) -> (int) (a.fitness - b.fitness));
            }

            // crossover, produce offspring
            {
                final int n = results.size();
                // 0.2 is the top percent of the population that reproduces
                final int top_n = (int)((float)n * 0.2f);

                List<Genome<E>> newPopulation = new ArrayList<>();

                for(int i=0; i<top_n; i++){
                    final Genome<E> parentAGenome = results.get(i).genome;

                    newPopulation.add(parentAGenome);
                    for(int j=0; j<top_n; j++){
                        if(i == j)continue;
                        final Genome<E> parentBGenome = results.get(j).genome;

                        //TODO: amount of children can be configured, its a parameter
                        final Genome<E> child1 = parentAGenome.crossover(parentBGenome);

                        newPopulation.add(child1);
                    }
                }

                population = newPopulation;
            }

            // mutations
            {
                for(Genome<E> genome : population){
                    if(Utils.chance(0.10f))genome.mutate(0.05f);
                }
            }

        }
    }

}
