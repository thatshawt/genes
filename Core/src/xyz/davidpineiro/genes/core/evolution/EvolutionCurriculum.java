package xyz.davidpineiro.genes.core.evolution;

import xyz.davidpineiro.genes.core.evolution.exceptions.EvolutionTickLimitException;
import xyz.davidpineiro.genes.core.evolution.std.strings.CharGenome;
import xyz.davidpineiro.genes.core.evolution.std.strings.StringEvolutionProblem;
import xyz.davidpineiro.genes.core.problems.CurriculumSolver;
import xyz.davidpineiro.genes.core.problems.Curriculum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EvolutionCurriculum<G extends IGene>
        extends ArrayList<Curriculum.ProblemNode<GeneticEvolutionProblem<G>, Genome<G>>>
        implements Curriculum<GeneticEvolutionProblem<G>, Genome<G>> {

    public void add(GeneticEvolutionProblem<G> problem){
        ProblemNode<GeneticEvolutionProblem<G>, Genome<G>> node = new ProblemNode<>(problem);
        this.add(node);
    }

    public static class Solver<G extends IGene>
            implements CurriculumSolver<GeneticEvolutionProblem<G>, Genome<G>> {

        private final EvolverSolver<G> evolverSolver;

        public Solver(EvolverSolver<G> evolverSolver) {
            this.evolverSolver = evolverSolver;
        }

        @Override
        public void solve(Curriculum<GeneticEvolutionProblem<G>, Genome<G>> curriculum){
            for(ProblemNode<GeneticEvolutionProblem<G>, Genome<G>> node : curriculum) {
                //solve the problem
                EvolverSolver.ReturnReason returnReason = evolverSolver.solve(node.problem);
                node.solution = (Genome<G>) evolverSolver.solution.clone();

                System.out.printf("return reason: %s\n", returnReason.name());

                final int n = evolverSolver.cMAX_PARENTS;
                Genome<G>[] newInit = new Genome[n];
                for(int i=0;i<n;i++){
                    newInit[i] = (Genome<G>) node.solution.clone();
                }
                //make sure next problem gets benefits from previous problem
                evolverSolver.initPopulation = List.of(newInit);
            }
        }
    }

    public static void main(String[] args) {
        EvolverSolver<CharGenome.CharGene> evolverSolver =
                new EvolverSolver<>(
                CharGenome.CharGene::getRandom,
                CharGenome::getRandomGenome
        );
        EvolutionCurriculum.Solver<CharGenome.CharGene> curriculumSolver =
                new EvolutionCurriculum.Solver<>(evolverSolver);

        EvolutionCurriculum<CharGenome.CharGene> curriculum = new EvolutionCurriculum<>();
        curriculum.add(new StringEvolutionProblem("hello"));
        curriculum.add(new StringEvolutionProblem("hello-world"));

        curriculumSolver.solve(curriculum);

//        System.out.printf("hello-wo=ld8||EEE fitness: %f\n",
//                new StringEvolutionProblem("hello-world")
//                        .fitness(CharGenome.fromString("hello-wo=ld8||EEE")));
    }

}
