package xyz.davidpineiro.genes.core.evolution.std;

import xyz.davidpineiro.genes.core.evolution.GeneticEvolutionProblem;
import xyz.davidpineiro.genes.core.evolution.Genome;

public class StringEvolutionProblem extends GeneticEvolutionProblem<CharGene> {

    public final String targetString;

    public StringEvolutionProblem(String targetString) {
        this.targetString = targetString;
    }

    @Override
    protected float fitness(Genome<CharGene> genome) {
        int count = 0;
        for(int i=0;i<genome.size();i++){
            if(i >= targetString.length()){
                count--;
                continue;
            }

            final CharGene gene = genome.get(i);
            final char target = targetString.charAt(i);

            if(gene.getValue() == target)count++;
            else count--;
        }
        return (float)count;
    }
}
