package xyz.davidpineiro.genes.core.evolution;

import xyz.davidpineiro.genes.core.Utils;
import xyz.davidpineiro.genes.core.evolution.asm.registerMachine.RegisterMachine;
import xyz.davidpineiro.genes.core.evolution.asm.registerMachine.RegisterMachineGenome;
import xyz.davidpineiro.genes.core.evolution.exceptions.EvolutionTickLimitException;
import xyz.davidpineiro.genes.core.problems.Solver;

import java.util.ArrayList;
import java.util.List;

public class EvolverSolver<G extends IGene>
        implements Solver<GeneticEvolutionProblem<G>, EvolverSolver.ReturnReason> {

    public enum ReturnReason{
        MAX_TICK,SATISFIES
    }

    /** TODO (s)
    - add a BioEvolverSolver -> (Codon, Ribosome),
        inspired directly from ribosomes and amino acids triplet coding.
        there should be start and stop codons and stuff.
     */

    public int cMAX_PARENTS = 15;
    public long cMAX_TICK_LIMIT = 100_000;
    public int cINIT_RANDOMS = 5000;
    public float cTOP_PERCENT_PARENTS = 0.2f;
    public int cCHILDREN = 4;

    public GeneFactory<G> geneFactory;
    public GenomeFactory<G> genomeFactory;

    public List<Genome<G>> initPopulation = new ArrayList<>();
    public Genome<G> solution;
    public Object thing;

    public EvolverSolver(GeneFactory<G> geneFactory, GenomeFactory<G> genomeFactory) {
        this.geneFactory = geneFactory;
        this.genomeFactory = genomeFactory;
    }

    public EvolverSolver(DualGeneGenomeFactory<G> dualGeneGenomeFactory){
        this.geneFactory = dualGeneGenomeFactory;
        this.genomeFactory = dualGeneGenomeFactory;
    }

    private static class GenomeResult<E extends IGene>{
        public final Genome<E> genome;
        public final float fitness;
//        public final String id = Utils.getRandomPrintableString(7);
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

    public ReturnReason solve(GeneticEvolutionProblem<G> problem) {
        List<Genome<G>> population = new ArrayList<>(initPopulation);

        //make some genomes
        for(int i = 0; i<this.cINIT_RANDOMS; i++){
            population.add(genomeFactory.randomGenome());
        }

        long tick = 0;
        while(tick <= this.cMAX_TICK_LIMIT){
            tick++;
            //get fitness of all our genomes rn
            List<GenomeResult<G>> results = new ArrayList<>();

            boolean satisfied = false;
            // get fitnessesese
            {
                for (final Genome<G> genome : population) {
                    final float fitness = problem.fitness(genome);
                    satisfied = problem.satisfies(fitness, genome);

                    results.add(new GenomeResult<>(genome, fitness));

                    if(satisfied){
                        solution = (Genome<G>) genome.clone();
//                        return ReturnReason.SATISFIES;
                        break;
                    }
                }

                //sort by fitness
                results.sort((a, b) -> {
                    int fitness = (int) (b.fitness - a.fitness);
                    int size = (a.genome.size() - b.genome.size());
                    //if they are equal in fitness sort by size
                    if(fitness == 0)return size;
                    else return fitness; //otherwise sort by the fitter one
                });
            }
            if(tick == cMAX_TICK_LIMIT){
                solution = results.get(0).genome;
                return ReturnReason.MAX_TICK;
            }

            //run this after results so we cann see how we did
            if(tick % 1000 == 0 || satisfied) {
                System.out.printf(
                        "tick: %d, population_size: %d, best1: %s\n",
                        tick, population.size(), results.get(0));
                if(thing instanceof RegisterMachine rm){
                    rm.state.prettyPrintRegs();
                }
            }
//            printPopulation(population);

            if(satisfied){
                return ReturnReason.SATISFIES;
            }

            // crossover, produce offspring
            {
                final int n = results.size();
                final int top_n = Math.min((int)((float)n * this.cTOP_PERCENT_PARENTS), this.cMAX_PARENTS);
//                System.out.printf("topn: %d\n", top_n);

                List<Genome<G>> newPopulation = new ArrayList<>();

                for(int i=0; i<top_n; i++){
                    final GenomeResult<G> parentAGenomeResult = results.get(i);

                    newPopulation.add(parentAGenomeResult.genome);
                    for(int j=0; j<top_n; j++){
                        if(i == j)continue;
                        final GenomeResult<G> parentBGenomeResult = results.get(j);

                        for(int i1 = 0; i1< this.cCHILDREN; i1++){
                            final Genome<G> child = parentAGenomeResult.genome.crossover(parentBGenomeResult.genome);
                            newPopulation.add(child);
                        }

                    }
                }

                population = newPopulation;
            }

            // mutations
            {
                for(Genome<G> genome : population){
                    if(Utils.chance(0.10f))genome.mutate(geneFactory);
                }
            }


        }
        return null;
    }

    private static <E extends IGene> void  printPopulation(List<Genome<E>> population){
        System.out.printf("population: {\n");
        for(Genome<?> genome : population){
            System.out.printf("%s, ", genome);
        }
        System.out.printf("}\n");
    }

}
