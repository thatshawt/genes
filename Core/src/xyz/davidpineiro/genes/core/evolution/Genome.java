package xyz.davidpineiro.genes.core.evolution;

import xyz.davidpineiro.genes.core.Utils;

import java.util.LinkedList;
import java.util.ListIterator;

public abstract class Genome<G extends IGene> extends LinkedList<G> implements Cloneable{

    public float cINSERT_GENE_CHANCE = 0.05f;
    public float cADD_GENE_CHANCE = 0.01f;
    public float cDELETE_GENE_CHANCE = 0.005f;
    public float cGENE_MUTATE_CHANCE= 0.05f;
    public float cFLIP_ACTIVE_CHANCE = 0.01f;

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

    @Override
    public Object clone() {
        Genome<G> genome = getEmpty();

        for(G gene : this){
            genome.add((G) gene.clone());
        }
        return genome;
    }
}
