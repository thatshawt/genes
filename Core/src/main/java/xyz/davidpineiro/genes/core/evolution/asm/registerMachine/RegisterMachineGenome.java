package xyz.davidpineiro.genes.core.evolution.asm.registerMachine;

import xyz.davidpineiro.genes.core.evolution.Genome;

import java.util.ArrayList;
import java.util.List;

public class RegisterMachineGenome extends Genome<RegisterMachineGene> {

    @Override
    protected Genome<RegisterMachineGene> getEmpty() {
        return new RegisterMachineGenome();
    }

    public List<RegisterMachine.Assembler.CompleteInstruction> getInstructions(){
        List<RegisterMachine.Assembler.CompleteInstruction> instructions = new ArrayList<>();

        this.forEach(gene -> {
            instructions.add(gene.getCompleteInstruction());
        });
        return instructions;
    }

    @Override
    public String toString() {
        StringBuilder prettyInstructions = new StringBuilder();

        for(RegisterMachineGene gene : this)
            prettyInstructions.append(gene.toString() + "\n");

        return String.format("RegisterMachineGenome{\n%s}", prettyInstructions);
    }

    public static void main(String[] args) throws ClassNotFoundException {
        Class.forName("xyz.davidpineiro.genes.core.evolution.asm.registerMachine.RegisterMachine");

        RegisterMachineGene gene1 = new RegisterMachineGene(
                new RegisterMachine.Assembler.CompleteInstruction(RegisterMachine.Instruction.nop, new RegisterMachine.OpContext.Argument[]{}),
                new RegisterMachine.Instruction[]{}
        );

        RegisterMachineGene gene2 = (RegisterMachineGene) gene1.clone();

        System.out.printf("gene1:'%s', gene2:'%s'",gene1, gene2);

        gene1.mutate();

        System.out.printf("gene1:'%s', gene2:'%s'",gene1, gene2);
    }

}
