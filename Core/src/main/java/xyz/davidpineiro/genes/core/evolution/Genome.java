package xyz.davidpineiro.genes.core.evolution;

import xyz.davidpineiro.genes.core.Utils;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Genome<G extends IGene> extends LinkedList<G> implements Cloneable{

    public float cINSERT_GENE_CHANCE = 0.008f;
    public float cADD_GENE_CHANCE = 0.008f;
    public float cDELETE_GENE_CHANCE = 0.01f;
    public float cGENE_MUTATE_CHANCE= 0.05f;
    public float cFLIP_ACTIVE_CHANCE = 0.01f;
    public float cMAX_GENES = 10;

    public final String RANDOM_ID = Utils.getRandomPrintableString(100);

    @Override
    public int hashCode() {
        return RANDOM_ID.chars().sum();
    }

    public int activeGenes(){
        int count = 0;
        for(IGene gene : this){
            if(gene.isActive())count++;
        }
        return count;
    }

    public int inactiveGenes(){
        int count = 0;
        for(IGene gene : this){
            if(!gene.isActive())count++;
        }
        return count;
    }

    protected abstract Genome<G> getEmpty();

    public Genome<G> crossover(Genome<G> other) {
        /*
        geneA: 32123
        geneB: 23123123123
        we cut right^here
         */

        Genome<G> resultGenome = getEmpty();

        final Genome<G> longerGenome = other.size() >= this.size() ? other : this;
        final Genome<G> smallerGenome = other.size() <= this.size() ? other : this;

        //this is uniform crossover, idk, its the easiest to implement i guess
        for(int i=0;i<smallerGenome.size();i++){
            if(Utils.chance(0.5f)) resultGenome.add((G) longerGenome.get(i).clone());
            else resultGenome.add((G) smallerGenome.get(i).clone());
        }
        for(int i=smallerGenome.size();i<longerGenome.size();i++){
            resultGenome.add((G) longerGenome.get(i).clone());
        }

        return resultGenome;

    }

    void mutate(GeneFactory<G> geneFactory){
        ListIterator<G> iter = this.listIterator();

        if(Utils.chance(cADD_GENE_CHANCE)){
            iter.add(geneFactory.randomGene());
//            System.out.println("added gene");
        }

        while(iter.hasNext()){
            G next = iter.next();
            try {
                if (Utils.chance(cINSERT_GENE_CHANCE)){
                    iter.add(geneFactory.randomGene());
//                    System.out.println("inserted gene");
                }
                if (Utils.chance(cDELETE_GENE_CHANCE)){
                    iter.remove();
//                    System.out.println("remove gene");
                }
                if(Utils.chance(cGENE_MUTATE_CHANCE)){
                    next.mutate();
                }
                if(Utils.chance(cFLIP_ACTIVE_CHANCE)){
                    next.flipActive();
                }
            }catch(IllegalStateException ignore){
//                System.out.print("{illegalstate}\n");
            }
        }
    }

    public Genome<G> naivePrune(GeneticEvolutionProblem<G> problem,
                                int consecutiveFailsThreshold){
        final Random random = ThreadLocalRandom.current();

        int consecutiveFails = 0;
        Genome<G> genome = (Genome<G>) this.clone();
        Genome<G> genome2 = (Genome<G>) genome.clone();

        while(consecutiveFails < consecutiveFailsThreshold){
            //remove a random gene
            genome2.remove(random.nextInt(genome2.size()));

            //try new genome on problem
//            final float fitness1 = problem.fitness(genome);
//            final boolean satisfies1 = problem.satisfies(fitness1, genome);

            final float fitness2 = problem.fitness(genome2);
            final boolean satisfies2 = problem.satisfies(fitness2, genome2);

//            System.out.printf(" (%s,%s) ", satisfies1 ? "T" : "F", satisfies2 ? "T" : "F");

            //if it didnt fail, then we update use it for the next run
            if(satisfies2){
                consecutiveFails = 0;
                genome = (Genome<G>) genome2.clone();
            }else{//if it makes it fail, the problem then add to the counter and try again
                genome2 = (Genome<G>) genome.clone();
                consecutiveFails++;
            }
        }
        return genome;
    }

    @Override
    public Object clone() {
        Genome<G> genome = getEmpty();

        for(G gene : this){
            genome.add((G) gene.clone());
        }
        return genome;
    }
}
