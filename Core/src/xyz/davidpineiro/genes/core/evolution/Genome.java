package xyz.davidpineiro.genes.core.evolution;

import xyz.davidpineiro.genes.core.Utils;

import java.util.LinkedList;
import java.util.ListIterator;

public abstract class Genome<E extends Gene> extends LinkedList<E> {

    public float cINSERT_GENE_CHANCE = 0.05f;
    public float cADD_GENE_CHANCE = 0.01f;
    public float cDELETE_GENE_CHANCE = 0.01f;
    public float cGENE_MUTATE_CHANCE= 0.05f;
    public float cFLIP_ACTIVE_CHANCE = 0.01f;

    public int activeGenes(){
        int count = 0;
        for(Gene gene : this){
            if(gene.isActive())count++;
        }
        return count;
    }

    public int inactiveGenes(){
        int count = 0;
        for(Gene gene : this){
            if(!gene.isActive())count++;
        }
        return count;
    }

    protected abstract Genome<E> getEmpty();

    public Genome<E> crossover(Genome<E> other) {

        /*
        geneA: 32123
        geneB: 23123123123
        we cut right^here
         */

        Genome<E> resultGenome = getEmpty();

        final Genome<E> longerGenome = other.size() >= this.size() ? other : this;
        final Genome<E> smallerGenome = other.size() <= this.size() ? other : this;

        //this is uniform crossover, idk its the easiest to implement i guess
        for(int i=0;i<smallerGenome.size();i++){
            if(Utils.chance(0.5f)) resultGenome.add((E) longerGenome.get(i).clone());
            else resultGenome.add((E) smallerGenome.get(i).clone());
        }
        for(int i=smallerGenome.size();i<longerGenome.size();i++){
            resultGenome.add((E) longerGenome.get(i).clone());
        }

        return resultGenome;

    }

    void mutate(GeneFactory<E> geneFactory){
        ListIterator<E> iter = this.listIterator();

        if(Utils.chance(cADD_GENE_CHANCE)){
            iter.add(geneFactory.randomGene());
//            System.out.println("added gene");
        }

        while(iter.hasNext()){
            E next = iter.next();
            try {
                if (Utils.chance(cINSERT_GENE_CHANCE)){
                    iter.add(geneFactory.randomGene());
//                    System.out.println("inserted gene");
                }
                if (Utils.chance(cDELETE_GENE_CHANCE)){
                    iter.remove();
//                    System.out.println("remove gene");
                }
                next.mutateGene(cGENE_MUTATE_CHANCE, cFLIP_ACTIVE_CHANCE);
            }catch(IllegalStateException ignore){
//                System.out.print("{illegalstate}\n");
            }
        }
    }


}
