package xyz.davidpineiro.genes.core;

import xyz.davidpineiro.genes.core.evolution.EvolverSolver;
import xyz.davidpineiro.genes.core.evolution.GeneticEvolutionProblem;
import xyz.davidpineiro.genes.core.evolution.Genome;
import xyz.davidpineiro.genes.core.evolution.exceptions.EvolutionTickLimitException;
import xyz.davidpineiro.genes.core.evolution.std.CharGenome;
import xyz.davidpineiro.genes.core.evolution.std.StringEvolutionProblem;

public class TestBench {

    public static void main(String[] args) {
        EvolverSolver<CharGenome.CharGene> evolverSolver = new EvolverSolver<>();

        final String theString = "super_long_string_for_super_poop";

        final GeneticEvolutionProblem<CharGenome.CharGene> problem = new StringEvolutionProblem(
                theString
        );

        Genome<CharGenome.CharGene> solution = null;
        try {
            solution = evolverSolver.solve(
                    problem,
                    CharGenome.CharGene::getRandom,
                    CharGenome::getRandomGenome
            );
        } catch (EvolutionTickLimitException e) {
            System.err.println(e);
        }

        System.out.printf("theString: '%s'\n" +
                "lenngth: %d\n" +
                "solution: '%s'\n" +
                "solution length: %d\n", theString, theString.length(), solution, solution.size());
    }

}
