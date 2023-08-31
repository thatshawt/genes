package xyz.davidpineiro.genes.core;

import xyz.davidpineiro.genes.core.evolution.EvolverSolver;
import xyz.davidpineiro.genes.core.evolution.GeneticEvolutionProblem;
import xyz.davidpineiro.genes.core.evolution.Genome;
import xyz.davidpineiro.genes.core.evolution.exceptions.EvolutionTickLimitException;
import xyz.davidpineiro.genes.core.evolution.std.strings.CharGenome;
import xyz.davidpineiro.genes.core.evolution.std.strings.StringEvolutionProblem;
import xyz.davidpineiro.genes.core.problems.Problem;

public class TestBench {

    public static void main(String[] args) {
        EvolverSolver<CharGenome.CharGene> evolverSolver = new EvolverSolver<>(
                CharGenome.CharGene::getRandom,
                CharGenome::getRandomGenome
        );

        final String theString = "super_long_string_for_super_poop";

        final GeneticEvolutionProblem<CharGenome.CharGene> problem =
                new StringEvolutionProblem(theString);

        Genome<CharGenome.CharGene> solution = CharGenome.fromString("goo goo gaa gaa");
        EvolverSolver.ReturnReason returnReason = evolverSolver.solve(problem);
        solution = evolverSolver.solution;

        System.out.printf("theString: '%s'\n" +
                "lenngth: %d\n" +
                "solution: '%s'\n" +
                "solution length: %d\n", theString, theString.length(), solution, solution.size());
    }

}
