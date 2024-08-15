package xyz.davidpineiro.genes.core.evolution.asm.registerMachine.problem;

import xyz.davidpineiro.genes.core.evolution.GeneticEvolutionProblem;
import xyz.davidpineiro.genes.core.evolution.Genome;
import xyz.davidpineiro.genes.core.evolution.asm.registerMachine.RegisterMachineGene;

public class RegisterMachineProgrammerProblem extends GeneticEvolutionProblem<RegisterMachineGene> {

    // given a set of problems
    // produce a program with type: problem -> solution
    // solver: problem -> solution



    @Override
    public float fitness(Genome<RegisterMachineGene> genome) {
        return 0;
    }

    @Override
    public boolean satisfies(float fitness, Genome<RegisterMachineGene> genome) {
        return false;
    }
}
