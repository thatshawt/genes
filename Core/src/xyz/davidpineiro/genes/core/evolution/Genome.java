package xyz.davidpineiro.genes.core.evolution;

import xyz.davidpineiro.genes.core.Utils;

import java.util.LinkedList;
import java.util.ListIterator;

public abstract class Genome<E extends Gene> extends LinkedList<E> {

    public float cINSERT_GENE_CHANCE = 0.01f;
    public float cADD_GENE_CHANCE = 0.01f;
    public float cDELETE_GENE_CHANCE = 0.01f;
    public float cGENE_MUTATE_CHANCE= 0.05f;

    public abstract Genome<E> crossover(Genome<E> other);
    void mutate(GeneFactory<E> geneFactory){
        ListIterator<E> iter = this.listIterator();

        if(Utils.chance(cADD_GENE_CHANCE))iter.add(geneFactory.randomGene());

        while(iter.hasNext()){
            E next = iter.next();
            try {
                if (Utils.chance(cINSERT_GENE_CHANCE)){
                    iter.add(geneFactory.randomGene());
//                    System.out.println("added gene");
                }
                if (Utils.chance(cDELETE_GENE_CHANCE)) iter.remove();
                if (Utils.chance(cGENE_MUTATE_CHANCE)) next.mutate();
            }catch(IllegalStateException ignore){
                System.out.print("{illegalstate}\n");
            }
        }
    }


}
