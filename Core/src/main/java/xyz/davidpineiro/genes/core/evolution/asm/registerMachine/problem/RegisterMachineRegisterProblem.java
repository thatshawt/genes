package xyz.davidpineiro.genes.core.evolution.asm.registerMachine.problem;

import xyz.davidpineiro.genes.core.TriFunction;
import xyz.davidpineiro.genes.core.evolution.*;
import xyz.davidpineiro.genes.core.evolution.asm.registerMachine.RegisterMachine;
import xyz.davidpineiro.genes.core.evolution.asm.registerMachine.RegisterMachineGene;
import xyz.davidpineiro.genes.core.evolution.asm.registerMachine.RegisterMachineGenome;
import xyz.davidpineiro.genes.core.problems.CompoundProblem;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class RegisterMachineRegisterProblem extends GeneticEvolutionProblem<RegisterMachineGene>{

    private final Function<RegisterMachine.State, Float> fitness;
    private final TriFunction<Float, RegisterMachineGenome, RegisterMachine.State, Boolean> satisfies;
    private final Function<Integer, Integer> maxSteps;
    private RegisterMachine.State state;
    private RegisterMachine registerMachine;
    public RegisterMachine.Instruction[] blacklist;

    public RegisterMachineRegisterProblem(Function<RegisterMachine.State, Float> fitness,
                                          TriFunction<Float, RegisterMachineGenome, RegisterMachine.State,Boolean> satisfies,
                                          Function<Integer, Integer> maxSteps,
                                          RegisterMachine registerMachine,
                                          RegisterMachine.Instruction[] blacklist) {
        this.fitness = fitness;
        this.satisfies = satisfies;
        this.maxSteps = maxSteps;
        this.registerMachine = registerMachine;
        this.blacklist = blacklist;
    }

    @Override
    public float fitness(Genome<RegisterMachineGene> genome) {
        try {
            List<RegisterMachine.Assembler.CompleteInstruction> instructions =
                    ((RegisterMachineGenome)genome).getInstructions();

//            System.out.println(instructions);

            registerMachine.resetStateAndLoadProgram(instructions);

            registerMachine.state.maxSteps = this.maxSteps.apply(instructions.size());
            //disable syscall 0 for now (printing to console)
            registerMachine.state.nativeCallMap.put("sys.println", (state) -> {});

            registerMachine.stepUntilHalt();

            this.state = registerMachine.state;

            return this.fitness.apply(registerMachine.state);
        } catch (RegisterMachine.InterruptException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean satisfies(float fitness,
                             Genome<RegisterMachineGene> genome) {
        return this.satisfies.apply(fitness, (RegisterMachineGenome)genome, this.state);
    }

    enum RegisterType{
        INT,STRING,BOOL,FLOAT,OBJ;
    }

    interface RegisterTripletFunction<I>{
        RegisterTriplet[] apply(I input);
    }

    record RegisterTriplet(RegisterType register, int regi, Object value){
        Object getRegVal(RegisterMachine.State state){
            return switch(register){
                case INT -> state.ireg[regi];
                case STRING -> state.sreg[regi];
                case BOOL -> state.breg[regi];
                case FLOAT -> state.freg[regi];
                case OBJ -> state.oreg[regi];
            };
        }
    }

    public static <I> RegisterMachineRegisterProblem createProblem(
            RegisterTripletFunction<I> registerTriplets,
            Supplier<I> inputSupplier,
            RegisterMachine registerMachinez,
            RegisterMachine.Instruction[] blacklist
    ){
        final I input = inputSupplier.get();
        final RegisterTriplet[] triplets = registerTriplets.apply(input);

        return new RegisterMachineRegisterProblem(
                //fitness function
                (state) -> {
                    float sum = 0;

                    for(RegisterTriplet triplet : triplets){
                        final RegisterType regType = triplet.register;
                        final Object actualValue = triplet.getRegVal(state);
                        final Object targetValue = triplet.value;
                        switch(regType){
                            case INT -> {
                                final int diff = Math.abs((int)actualValue - (int)targetValue);
                                    sum -= diff;
                            }
                            case FLOAT -> {
                                final float diff = Math.abs((float)actualValue - (float)targetValue);
                                    sum -= diff;
                            }
                            default -> sum += actualValue.equals(targetValue) ? 1 : -1;
                        }
                    }

                    return sum;
                },
                //satisfies function
                (fitness, genome, state) -> {
                    boolean sum = true;
                    for(RegisterTriplet triplet : triplets){
                        final Object actualValue = triplet.getRegVal(state);
                        final Object targetValue = triplet.value;
                        sum = sum && actualValue.equals(targetValue);
                    }
                    return sum;
                },
                //maxSteps
                (programLength) -> programLength*2,
                //the register machine we are going to use
                registerMachinez,
                blacklist
        );
    }

    static final class Factory implements DualGeneGenomeFactory<RegisterMachineGene>{
        public RegisterMachine.Instruction[] blacklist;
        public int n;

        public Factory(RegisterMachine.Instruction[] blacklist, int n) {
            this.blacklist = blacklist;
            this.n = n;
        }

        @Override
        public RegisterMachineGene randomGene() {
            RegisterMachineGene gene1 = new RegisterMachineGene(null,blacklist);
            gene1.mutate();
            return (RegisterMachineGene) gene1.clone();
        }

        @Override
        public Genome<RegisterMachineGene> randomGenome() {
            return GenomeFactory.getRandomGenome(new RegisterMachineGenome(), this, n);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        Class.forName("xyz.davidpineiro.genes.core.evolution.asm.registerMachine.RegisterMachine");

        RegisterMachine registerMachinez = new RegisterMachine();

        RegisterMachine.Instruction[] blacklist = new RegisterMachine.Instruction[]{
                RegisterMachine.Instruction.interrupt_enable
        };

        RegisterMachineRegisterProblem problem_10 = createProblem((input) ->
                    new RegisterTriplet[]{
                        new RegisterTriplet(RegisterType.INT, 0, 10)
                    },
                () -> null,
                registerMachinez,blacklist);

        final RegisterMachineRegisterProblem problem_11 = createProblem((input) ->
                        new RegisterTriplet[]{
                                new RegisterTriplet(RegisterType.INT, 1, 11)
                        },
                () -> null,
                registerMachinez,blacklist);

        final CompoundProblem<RegisterMachineGene> compoundProblem =
                new CompoundProblem<>(problem_10, problem_11);

        Factory factory = new Factory(blacklist, 10);

        EvolverSolver<RegisterMachineGene> evolverSolver = new EvolverSolver<>(factory);

        evolverSolver.thing = registerMachinez;

        System.out.printf("starting now...\n");

        EvolverSolver.ReturnReason returnReason = evolverSolver.solve(compoundProblem);
        RegisterMachineGenome solution = (RegisterMachineGenome)evolverSolver.solution;

        System.out.printf("return reason: %s, solution: \n%s\n, iregs: %s\n",
                returnReason, solution, Arrays.toString(registerMachinez.state.ireg)
        );

//        RegisterMachineGenome prunedSolution = (RegisterMachineGenome)
//                solution.naivePrune(compoundProblem, 10);
//
//        compoundProblem.fitness(prunedSolution);
//
//        System.out.printf("pruned solution: \n%s\n, iregs: %s\n",
//                prunedSolution, Arrays.toString(registerMachinez.state.ireg)
//        );

    }



}
