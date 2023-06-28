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
        for(int i=0;i<genome.size();i++){
            if(i >= targetString.length()){
                count--;
                continue;
            }

            final CharGenome.CharGene gene = genome.get(i);
            final char target = targetString.charAt(i);

            if(gene.getValue() == target)count++;
            else count--;
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
