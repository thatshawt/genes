package xyz.davidpineiro.genes.core.evolution.std.strings;

import xyz.davidpineiro.genes.core.evolution.GeneticEvolutionProblem;
import xyz.davidpineiro.genes.core.evolution.Genome;

public class StringEvolutionProblem extends GeneticEvolutionProblem<CharGenome.CharGene> {
    public final String targetString;

    public StringEvolutionProblem(String targetString) {
        this.targetString = targetString;
    }

    @Override
    public float fitness(Genome<CharGenome.CharGene> genome) {
        int count = 0;

        int genomei = 0, stringi = 0;
        while(genomei < genome.size()){
            final CharGenome.CharGene gene = genome.get(genomei);
            if(!gene.isActive()){
                genomei++;
                continue;
            }

            if(stringi < targetString.length()) {
                final char geneChar = gene.getValue();
                final char stringChar = targetString.charAt(stringi);

                if (geneChar == stringChar)
                    count++;
                else
                    count--;

                stringi++;
                genomei++;
            }else{
                genomei++;
                count--;
            }
        }

        return (float)count;
    }

    protected boolean satisfies(float fitness, Genome<CharGenome.CharGene> genome) {
//        System.out.printf("fitness: %f, strlength: %d\n", fitness, targetString.length());
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
