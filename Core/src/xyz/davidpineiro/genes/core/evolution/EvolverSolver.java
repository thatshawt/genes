package xyz.davidpineiro.genes.core.evolution;

import xyz.davidpineiro.genes.core.Utils;
import xyz.davidpineiro.genes.core.evolution.exceptions.EvolutionTickLimitException;
import xyz.davidpineiro.genes.core.evolution.std.CharGenome;

import java.util.ArrayList;
import java.util.List;

public class EvolverSolver<E extends Gene> {

    public int cMAX_PARENTS = 10;
    public int cMAX_TICK_LIMIT = 10_000;
    public int cINIT_NUM = 1000;
    public float cTOP_PERCENT_PARENTS = 0.2f;
    public int cCHILDREN = 1;

    private static class GenomeResult<E extends Gene>{
        public final Genome<E> genome;
        public final float fitness;
        public final String id = Utils.getRandomPrintableString(7);
//        public final boolean satisfies;

        public GenomeResult(Genome<E> genome,
                            float fitness
//                            boolean satisfies
        ) {
            this.genome = genome;
            this.fitness = fitness;
//            this.satisfies = satisfies;
        }

        @Override
        public String toString() {
            return "GenomeResult{" +
                    "genome=" + genome +
                    ", fitness=" + fitness +
//                    ", id='" + id + '\'' +
                    '}';
        }
    }

    public Genome<E> solve(GeneticEvolutionProblem<E> problem,
                                            GeneFactory<E> geneFactory,
                                            GenomeFactory<E> genomeFactory) throws EvolutionTickLimitException {
        List<Genome<E>> population = new ArrayList<>();

        //make 100 genomes idk
        for(int i=0;i<this.cINIT_NUM;i++){
            population.add(genomeFactory.randomGenome());
        }

        int tick = 0;
        while(tick <= this.cMAX_TICK_LIMIT){
            tick++;
            //get fitness of all our genomes rn
            List<GenomeResult<E>> results = new ArrayList<>();

            // get fitnessesese
            {
                for (final Genome<E> genome : population) {
                    final float fitness = problem.fitness(genome);
                    final boolean satisfied = problem.satisfies(fitness, genome);

                    if(satisfied){
                        return genome;
                    }

                    results.add(new GenomeResult<E>(genome, fitness));

                }

                //sort by fitness
                results.sort((a, b) -> (int) (b.fitness - a.fitness));
            }

            //run this after results so we cann see how we did
            System.out.printf(
                    "tick: %d, population_size: %d, best1: %s\n",
                    tick, population.size(), results.get(0));
//            printPopulation(population);

            // crossover, produce offspring
            {
                final int n = results.size();
                // 0.2 is the top percent of the population that reproduces
                final int top_n = Math.min((int)((float)n * this.cTOP_PERCENT_PARENTS), this.cMAX_PARENTS);
                System.out.printf("topn: %d\n", top_n);

                List<Genome<E>> newPopulation = new ArrayList<>();

                for(int i=0; i<top_n; i++){
                    final GenomeResult<E> parentAGenomeResult = results.get(i);

                    newPopulation.add(parentAGenomeResult.genome);
                    for(int j=0; j<top_n; j++){
                        if(i == j)continue;
                        final GenomeResult<E> parentBGenomeResult = results.get(j);

                        for(int i1 = 0; i1< this.cCHILDREN; i1++){
                            final Genome<E> child = parentAGenomeResult.genome.crossover(parentBGenomeResult.genome);
                            newPopulation.add(child);
                        }

                    }
                }

                population = newPopulation;
            }

            // mutations
            {
                for(Genome<E> genome : population){
                    if(Utils.chance(0.10f))genome.mutate(geneFactory);
                }
            }


        }
        //if we go past the "tick limit"
        throw new EvolutionTickLimitException();
    }

    private static <E extends Gene> void  printPopulation(List<Genome<E>> population){
        System.out.printf("population: {\n");
        for(Genome<?> genome : population){
            System.out.printf("%s, ", genome);
        }
        System.out.printf("}\n");
    }

}
