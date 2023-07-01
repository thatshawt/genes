package xyz.davidpineiro.genes.core.evolution.std;

import xyz.davidpineiro.genes.core.evolution.GeneticEvolutionProblem;
import xyz.davidpineiro.genes.core.evolution.Genome;

public class StringEvolutionProblem extends GeneticEvolutionProblem<CharGenome.CharGene> {

    public final String targetString;

    public StringEvolutionProblem(String targetString) {
        this.targetString = targetString;
    }

    @Override
    protected float fitness(Genome<CharGenome.CharGene> genome) {
        int count = 0;

        int genomei = 0, stringi = 0;
        while(genomei < genome.size() && stringi < targetString.length()){
            final CharGenome.CharGene gene = genome.get(genomei);
            if(!gene.isActive()){
                genomei++;
                continue;
            }

            final char geneChar = gene.getValue();
            final char stringChar = targetString.charAt(stringi);

            if(geneChar == stringChar)
                count++;
            else
                count--;

            stringi++;
            genomei++;
        }

        return (float)count;
    }

    protected boolean satisfies(float fitness, Genome<CharGenome.CharGene> genome) {
        return (int)fitness == targetString.length();
    }

    public static void main(String[] args) {
        System.out.println("string problem fitness tesst");
        StringEvolutionProblem problem = new StringEvolutionProblem("poopy");
        float fitness = problem.fitness(CharGenome.fromString("poopy"));
        System.out.printf("poopy vs poopy: %f\n", fitness);

        fitness = problem.fitness(CharGenome.fromString("susssy"));
        System.out.printf("poopy vs susssy: %f\n", fitness);

        fitness = problem.fitness(CharGenome.fromString("poo"));
        System.out.printf("poopy vs poo: %f\n", fitness);
    }

}
