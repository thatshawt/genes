package xyz.davidpineiro.genes.core.evolution.asm.registerMachine;

import xyz.davidpineiro.genes.core.Utils;
import xyz.davidpineiro.genes.core.evolution.AbstractGene;
import xyz.davidpineiro.genes.core.evolution.IGene;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RegisterMachineGene extends AbstractGene {
    private RegisterMachine.Assembler.CompleteInstruction completeInstruction;
    private final RegisterMachine.Instruction[] instructionBlacklist;

    public RegisterMachineGene(RegisterMachine.Assembler.CompleteInstruction completeInstruction,
                               RegisterMachine.Instruction[] instructionBlacklist) {
        this.completeInstruction = completeInstruction;
        this.instructionBlacklist = instructionBlacklist;
    }

    public RegisterMachine.Assembler.CompleteInstruction getCompleteInstruction() {
        return completeInstruction;
    }

    public static RegisterMachine.Instruction opFromInt(
            RegisterMachine.Instruction[] instructionBlacklist,
            int x
    ){
        List<RegisterMachine.Instruction> filteredInstructions = List.of(RegisterMachine.Instruction.values());
        filteredInstructions.removeAll(List.of(instructionBlacklist));

        //used modulus to ensure that the index will always be in range
        final int i = Math.abs(x % (filteredInstructions.size()-1));

        final RegisterMachine.Instruction instruction = filteredInstructions.get(i);
        return instruction;
    }

    public static RegisterMachine.Instruction getRandomOP(
            RegisterMachine.Instruction[] instructionBlacklist
    ){
        final RegisterMachine.Instruction instruction;
        int randomNum;
        //select random instruction
        {
            final List<RegisterMachine.Instruction> possibleInstructions =
                    new ArrayList<>(Arrays.stream(RegisterMachine.Instruction.values()).toList());

            possibleInstructions.removeAll(Arrays.stream(instructionBlacklist).toList());

            randomNum = ThreadLocalRandom.current().nextInt(possibleInstructions.size());

            instruction = possibleInstructions.get(randomNum);
        }
        return instruction;
    }

    public static RegisterMachine.Assembler.CompleteInstruction randomCompleteInstructionFromOp(
            RegisterMachine.Instruction instruction
            ){
        RegisterMachine.Assembler.CompleteInstruction completeInstruction;
        int randomNum;

        RegisterMachine.SpecLanguage.Parser.ArgumentType[] argTypes;
        //choose random argument types
        {
            final String opcode = RegisterMachine.Instruction.opcodeMapB.get(instruction);

            final RegisterMachine.SpecLanguage.Parser.Argument[] requiredArgs =
                    RegisterMachine.Instruction.specInstructionMap.get(opcode);

            argTypes = new RegisterMachine.SpecLanguage.Parser.ArgumentType[requiredArgs.length];

            for (int i = 0; i < requiredArgs.length; i++) {
                final RegisterMachine.SpecLanguage.Parser.Argument arg = requiredArgs[i];
                final RegisterMachine.SpecLanguage.Parser.ArgumentType[] possibleArgs = arg.argumentType;

                randomNum = ThreadLocalRandom.current().nextInt(possibleArgs.length);
                argTypes[i] = possibleArgs[randomNum];
            }
        }

        //build random arguments with selected argument types
        RegisterMachine.OpContext.Argument[] arguments =
                new RegisterMachine.OpContext.Argument[argTypes.length];

        for (int i = 0; i < argTypes.length; i++) {
            final RegisterMachine.SpecLanguage.Parser.ArgumentType argType = argTypes[i];
            Object value;

            final Random random = ThreadLocalRandom.current();

            value = switch (argType) {
                case FLOAT_REG, INTEGER_REG, OBJECT_REG, STRING_REG, BOOL_REG ->
                        random.nextInt(RegisterMachine.State.REGISTER_COUNT);
                case BOOL_IMM -> random.nextBoolean();
                case FLOAT_IMM -> random.nextFloat();
                case INTEGER_IMM -> random.nextInt(10);
                case STRING_IMM -> {
                    if(random.nextFloat() < 0.5f) {
                        Set<String> standardCalls = RegisterMachine.StandardNativeCall.sysCallMap.keySet();
                        ArrayList<String> stuff = new ArrayList<>(standardCalls);
                        yield Utils.pickRandom(stuff);
                    }
                    else
                        yield Utils.getRandomPrintableString(3);
                }
            };

            arguments[i] = new RegisterMachine.OpContext.Argument(argType.toOpContext(), value);
        }

        //finally, update the instruction
        completeInstruction = new RegisterMachine.Assembler.CompleteInstruction(instruction, arguments);

        return completeInstruction;
    }

    @Override
    public void mutate() {
        final RegisterMachine.Instruction instruction = getRandomOP(instructionBlacklist);

        this.completeInstruction = randomCompleteInstructionFromOp(instruction);
    }

    @Override
    public String toString() {
        return (this.active ? "" : "!") + completeInstruction.prettyString();
    }

    @Override
    public IGene clone() {
        return new RegisterMachineGene(this.completeInstruction.clone(), instructionBlacklist);
    }

    public static void main(String[] args) throws ClassNotFoundException {
        Class.forName("xyz.davidpineiro.genes.core.evolution.asm.registerMachine.RegisterMachine");

        RegisterMachineGene gene1 = new RegisterMachineGene(
                new RegisterMachine.Assembler.CompleteInstruction(RegisterMachine.Instruction.nop,
                        new RegisterMachine.OpContext.Argument[]{}),
                new RegisterMachine.Instruction[]{}
        );

        RegisterMachineGene gene2 = (RegisterMachineGene) gene1.clone();

        System.out.printf("gene1:'%s', gene2:'%s'",gene1, gene2);

        gene1.mutate();

        System.out.printf("gene1:'%s', gene2:'%s'",gene1, gene2);
    }

}
