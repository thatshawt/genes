package xyz.davidpineiro.genes.core.evolution.asm.registerMachine;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RegisterMachine {

    public State state = new State();

    //TODO: add this, but just a List of strings, not a map
//    public static final Map<String, List<String>> randomStringMap = new HashMap<>();
//    private static void initRandomStringThing(String s){
//        if(!randomStringMap.containsKey(s))
//            randomStringMap.put(s, new ArrayList<>());
//    }

    private void execute(Assembler.CompleteInstruction instruction, boolean increaseIp, boolean handleInterrupt){
        try {
//            System.out.printf("%s, steps: %d, maxSteps: %d\n",
//                    instruction.prettyString(), state.steps, state.maxSteps);
            if(state.steps >= state.maxSteps){
                state.halt = true;
                return;
            }
            state.steps++;

            instruction.instruction.exec.accept(this, instruction.arguments, state);
            if(increaseIp)state.ip++;
        } catch (InterruptException e) {
//            state.istack.push(state.ip);
            interrupt(e.interrupt, handleInterrupt, increaseIp);
        }
    }

    private void interrupt(Interrupt interrupt, boolean handleInterrupt, boolean increaseIp){
        final String s = interrupt.s;

        if(!state.interrupt_mask_table.containsKey(s))
            state.interrupt_mask_table.put(s,true);

        if(handleInterrupt
                && state.interrupt_mask_table.get(s)
                && state.userCallMap.containsKey(s)) {
            try {
                //this could exceed the maximum stack size, hence the try-catch
                state.istack.push(state.ip);
            }catch(InterruptException ignore){}

            state.ip = state.userCallMap.get(s);
        }
        else if(increaseIp){
            state.ip++;
        }
    }

    private Optional<Assembler.CompleteInstruction> getCurrentInstruction() {
        final Object currentObjVal;
        try {
            currentObjVal = state.omem.get(state.ip);
            if(currentObjVal instanceof Assembler.CompleteInstruction instruction){
                return Optional.of(instruction);
            }
        } catch (InterruptException ignored) {}

        return Optional.empty();
    }

    //======START PUBLIC API STUFF======

    public void step() {
        Optional<Assembler.CompleteInstruction> instruction = getCurrentInstruction();
        if(instruction.isPresent())
            execute(instruction.get(), true, true);
        else {
            state.steps++;
            interrupt(Interrupt.INVALID_INSTRUCTION, true, true);
        }
    }

    public void stepUntilHalt() {
        while(
//                state.ip < state.program.length &&
                state.steps != state.maxSteps &&
                state.ip >= 0 &&
                !state.halt
             ){
            step();
        }
    }

    public void resetState(){
        state = new State();
    }

    public void resetStateAndLoadProgram(List<Assembler.CompleteInstruction> instructions)
            throws InterruptException {
        resetState();
//        state.program = instructions.toArray(new Assembler.CompleteInstruction[0]);
        for (int i = 0; i < instructions.size(); i++) {
            final Assembler.CompleteInstruction instruction = instructions.get(i);
            state.omem.set(i, instruction.clone());
        }
    }

//    public void executeAssembly(String input) throws Assembler.Parser.WrongTypeException {
//        List<Assembler.Lexer.Token> tokens = Assembler.Lexer.lex(input);
//        List<Assembler.CompleteInstruction> instructions = Assembler.Parser.parse(tokens);
//        for(Assembler.CompleteInstruction instruction : instructions){
//            execute(instruction, false, false);
//        }
//    }

//    public void stepUntilBreakpoint(){
//        while(state.ip < state.program.length || state.halt || breakpoints.contains(state.ip)){
//            step();
//        }
//    }
    //======END PUBLIC API STUFF======

    private static class FiniteString{
        private String string;
        public int MAXIMUM_LENGTH = 256;

        public FiniteString(String string) {
            this.string = string;
            clipString();
        }

        private void clipString(){
            if(this.string.length() > MAXIMUM_LENGTH)
                this.string = this.string.substring(0, MAXIMUM_LENGTH);
        }

        public void set(String string){
            this.string = string;
            clipString();
        }

        public String add(String string){
            this.string += string;
            clipString();
            return this.string;
        }

        public String get(){
            return string;
        }

        @Override
        public String toString() {
            return this.string;
        }
    }

    private static class MachineMemory<T>{
        private final SortedMap<Integer, T> memory = new TreeMap<>();
        private final Supplier<T> defaultValue;
        private final State state;

        public MachineMemory(Supplier<T> defaultValue, State state) {
            this.defaultValue = defaultValue;
            this.state = state;
        }

        public void set(int address, T val) throws InterruptException{
            rangeCheck(address);
            memory.put(address, val);
        }

        public T get(int address) throws InterruptException{
            rangeCheck(address);
            if(!memory.containsKey(address)) set(address, defaultValue.get());
            return memory.get(address);
        }

        private void rangeCheck(int i) throws InterruptException{
            if(i < 0 || i > this.state.MAX_MEMORY_ADDRESS)
                throw new InterruptException(Interrupt.MEMORY_OUT_OF_BOUNDS);
        }
    }

    private static class StringMachineMemory extends MachineMemory<String> {
        public StringMachineMemory(State state) {
            super(() -> "", state);
        }
    }
    private static class IntMachineMemory extends MachineMemory<Integer> {
        public IntMachineMemory(State state) {
            super(() -> 0, state);
        }
    }
    private static class FloatMachineMemory extends MachineMemory<Float> {
        public FloatMachineMemory(State state) {
            super(() -> 0.0f, state);
        }
    }
    private static class BooleanMachineMemory extends MachineMemory<Boolean> {
        public BooleanMachineMemory(State state) {
            super(() -> true, state);
        }
    }

    private static class MachineStack<T>{
        private final State state;
        private Stack<T> stack = new Stack<>();

        public MachineStack(State state) {
            this.state = state;
        }

        public T pop() throws InterruptException {
            if (stack.size() == 0)
                throw new InterruptException(Interrupt.POP_EMPTY_STACK);

            return stack.pop();
        }

        public void push(T val) throws InterruptException {
            if(stack.size() >= state.MAX_STACK_SIZE)
                throw new InterruptException(Interrupt.STACK_OVERFLOW);

            if(val == null)
                return;

            stack.push(val);
        }

        public void clear(){
            stack.clear();
        }
    }

    public static class InterruptException extends Exception{
        public final Interrupt interrupt;
        public InterruptException(Interrupt interrupt) {
            super(interrupt.name());
            this.interrupt = interrupt;
        }
    }

    public enum Interrupt{
        //exceptions
        INT_DIV_BY_ZERO("sys.int.int_div_zero"),
        FLOAT_DIV_BY_ZERO("sys.int.float_div_zero"),
        POP_EMPTY_STACK("sys.int.empty_pop"),
        MEMORY_OUT_OF_BOUNDS("sys.int.memory_out_of_bounds"),
        STACK_OVERFLOW("sys.int.stack_overflow"),

        SYSCALL_ARGUMENT_EXCEPTION("sys.int.syscall_arg_bad"),

        CONVERSION_EXCEPTION("sys.int.conversion_error"),

        INVALID_INSTRUCTION("sys.int.exec_invalid_instruction"),
        ;

        public final String s;
        Interrupt(String s){
            this.s = s;
        }

    }

    public enum StandardNativeCall {
        PRINTLN("sys.println", (state) -> {
//            throw new RuntimeException();
            System.out.printf("%s\n",state.sreg[0]);
        }),
        RAND_FLOAT("sys.utils.randFloat", (state) -> {
            final float freg0 = state.freg[0];
            if(freg0 > 0.0f)
                state.freg[0] = ThreadLocalRandom.current().nextFloat(freg0);
        }),
        RAND_INT("sys.utils.randInt", (state)->{
            final int ireg0 = state.ireg[0];
            if(ireg0 > 0)
                state.ireg[0] = ThreadLocalRandom.current().nextInt(ireg0);
        }),

        ASSEMBLE("sys.asm.assemble", (state)->{
            try{
                state.oreg[0] = (RegisterMachine.Assembler.assemble(state.sreg[0].get()));
                state.breg[0] = (true);
            }catch(Exception e){
                state.breg[0] = (false);
            }
        }),

        OP_FROM_INT("asm.instruction.opFromInt", (state) -> {
            try{
                state.oreg[0] = RegisterMachineGene.opFromInt(new Instruction[]{}, state.ireg[0]);
                state.breg[0] = true;
            }catch(Exception ignored){
                state.breg[0] = false;
            }
        }),

        RANDOM_OP("asm.instruction.randomOp", (state) -> {
            state.oreg[0] = RegisterMachineGene.getRandomOP(new Instruction[]{});
        }),

        RANDOM_COMPLETE_FROM_OP("asm.instruction.randomCompleteFromOp", (state) -> {
            if(state.oreg[0] instanceof Instruction instruction) {
                state.oreg[0] = RegisterMachineGene.randomCompleteInstructionFromOp(instruction);
                state.breg[0] = true;
            }else{
                state.breg[0] = false;
            }
        }),


        ;

//        public final int i;
        public final String s;
        public final Consumer<State> exec;
        StandardNativeCall(String s, Consumer<State> exec){
//            this.i = i;
            this.s = s;
            this.exec = exec;
        }

        public static final Map<String, Consumer<State>> sysCallMap = new HashMap<>();

        static{
            for(StandardNativeCall systemCall : StandardNativeCall.values()) {
                sysCallMap.put(systemCall.s, systemCall.exec);
            }
        }

    }

    public static class State{
        State(){
            Arrays.fill(sreg, new FiniteString(""));
            this.nativeCallMap.putAll(StandardNativeCall.sysCallMap);
        }

        public long maxSteps = -1;
        public long steps = 0;
        public int ip = 0;
        public boolean halt = false;

        public static final int REGISTER_COUNT = 5;

        public final Map<String, Consumer<State>> nativeCallMap = new HashMap<>();
        public final Map<String, Integer> userCallMap = new HashMap<>();
//        public final Map<String, Integer> interruptMap = new HashMap<>();

//        public int[] interrupt_handler_table = new int[Interrupt.values().length];
        public Map<String, Boolean> interrupt_mask_table = new HashMap<>();

        public int[] ireg = new int[REGISTER_COUNT];
        public boolean[] breg = new boolean[REGISTER_COUNT];
        public float[] freg = new float[REGISTER_COUNT];
        public FiniteString[] sreg = new FiniteString[REGISTER_COUNT];
        public Object[] oreg = new Object[REGISTER_COUNT];

        public int MAX_MEMORY_ADDRESS = 2048;
        public IntMachineMemory imem = new IntMachineMemory(this);
        public BooleanMachineMemory bmem = new BooleanMachineMemory(this);
        public FloatMachineMemory fmem = new FloatMachineMemory(this);
        public StringMachineMemory smem = new StringMachineMemory(this);
        public MachineMemory<Object> omem = new MachineMemory<>(Object::new, this);

        public int MAX_STACK_SIZE = 2048;
        public MachineStack<Integer> istack = new MachineStack<>(this);
        public MachineStack<Boolean> bstack = new MachineStack<>(this);
        public MachineStack<Float> fstack = new MachineStack<>(this);
        public MachineStack<FiniteString> sstack = new MachineStack<>(this);
        public MachineStack<Object> ostack = new MachineStack<>(this);

        public void prettyPrintRegs(){
            System.out.printf("iregs: '%s'\n", Arrays.toString(ireg));
            System.out.printf("sregs: '%s'\n", Arrays.toString(sreg));
            System.out.printf("fregs: '%s'\n", Arrays.toString(freg));
            System.out.printf("bregs: '%s'\n", Arrays.toString(breg));
            System.out.printf("oregs: '%s'\n", Arrays.toString(oreg));
        }

    }

    public static class OpContext {
        enum ArgumentType{//9 different ones
            IREG, BREG, FREG, SREG, OREG,
            IIMM, BIMM, FIMM, SIMM,

            UNRESOLVED_LABEL
            ;

            public SpecLanguage.Parser.ArgumentType toSpecLang(){
                switch (this){
                    case SIMM: return SpecLanguage.Parser.ArgumentType.STRING_IMM;
                    case SREG: return SpecLanguage.Parser.ArgumentType.STRING_REG;
                    case BIMM: return SpecLanguage.Parser.ArgumentType.BOOL_IMM;
                    case BREG : return SpecLanguage.Parser.ArgumentType.BOOL_REG;
                    case FIMM : return SpecLanguage.Parser.ArgumentType.FLOAT_IMM;
                    case FREG : return SpecLanguage.Parser.ArgumentType.FLOAT_REG;

                    case UNRESOLVED_LABEL:
                    case IIMM : return SpecLanguage.Parser.ArgumentType.INTEGER_IMM;
                    case IREG : return SpecLanguage.Parser.ArgumentType.INTEGER_REG;
                    case OREG : return SpecLanguage.Parser.ArgumentType.OBJECT_REG;

//                    case EREG : return SpecLanguage.Parser.ArgumentType.ERROR_REG;
                    default: return null;
                }
            }

        }

        public record Argument(ArgumentType argumentType, Object value) {

            public Argument clone() {
        //                final Object newValue;
        //                switch(value){
        //                    case Integer i -> newValue = this.value;
        //                    default -> newValue = this.value;
        //                }
                return new Argument(this.argumentType, this.value);
            }

            @Override
            public String toString() {
                return "Argument{" +
                        "argumentType=" + argumentType +
                        ", value=" + value +
                        '}';
            }

            public int getInt() {
                return (int) value;
            }

            public boolean getBool() {
                return (boolean) value;
            }

            public String getString() {
                return (String) value;
            }

            public float getFloat() {
                return (float) value;
            }

            public Object getObj() {
                return value;
            }

            public int getSize() {
                return switch (argumentType) {
                    case SIMM -> getString().length();
                    case BIMM -> 1;
                    default -> 4;
                };
            }
        }
        public final RegisterMachine rm;
        public final Argument[] arguments;

        public OpContext(final RegisterMachine rm, final Argument[] arguments){
            this.rm = rm;
            this.arguments = arguments;
        }
    }

    public enum Instruction{
        //special instructions,
        nop("nop", (rm,args,state) -> {}),

        halt("halt", (rm,args,state) -> {
            state.halt = true;
        }),

        //interrupt instructions
        interrupt_set("int_set simm ireg/iimm", (rm,args,state) -> {
            final String interruptI = args[0].getString();
            final int ip = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();

            state.userCallMap.put(interruptI, ip);
        }),

        interrupt_enable("int_enable simm", (rm,args,state)->{
            final String interruptI = args[0].getString();

            state.interrupt_mask_table.put(interruptI, true);
        }),

        interrupt_disable("int_disable simm", (rm,args,state)->{
            final String interruptI = args[0].getString();

            state.interrupt_mask_table.put(interruptI, false);
        }),

        procedure_entry("registercall simm iimm", (rm,args,state) -> {
            final String arg0 = args[0].getString();
            final int arg1 = args[1].getInt();
            state.userCallMap.put(arg0, arg1);
        }),
        delete_procedure("deletecall simm", (rm,args,state) -> {
            final String arg0 = args[0].getString();
            state.userCallMap.remove(arg0);
        }),
        call("call sreg/simm", (rm,args,state) -> {
            final String callI;
            if(args[0].argumentType == OpContext.ArgumentType.SREG){
                callI = state.sreg[args[0].getInt()].get();
            }else {
                callI = args[0].getString();
            }

            if(state.nativeCallMap.containsKey(callI))
                state.nativeCallMap.get(callI).accept(state);
            else if(state.userCallMap.containsKey(callI)) {
                final int goingToIp = state.userCallMap.get(callI);
                state.istack.push(state.ip);//push return ip
                state.ip = goingToIp;
            }else{
                //TODO: interrupt here?
            }
        }),
        //"return" is a keyword so i used "returnn" instead.
        returnn("ret", (rm,args,state) -> {
            state.ip = state.istack.pop();
        }),

        ip_load("ipload ireg", (rm,args,state) -> {
            state.ireg[args[0].getInt()] = state.ip;
        }),

        ip_push("ippush", (rm,args,state) -> {
            state.istack.push(state.ip);
        }),

        //branching instructions
        jump("jmp iimm/ireg", (rm,args,state) -> {
            if(args[0].argumentType == OpContext.ArgumentType.IREG){
                state.ip += state.ireg[args[0].getInt()];
            }else if(args[0].argumentType == OpContext.ArgumentType.IIMM){
                state.ip += args[0].getInt();
            }
        }),

        jump_long("jmpl iimm/ireg", (rm,args,state) -> {
            switch (args[0].argumentType) {
                case IREG -> state.ip = state.ireg[args[0].getInt()];
                case IIMM -> state.ip = args[0].getInt();
            }
        }),

        jump_if_true("jmpt breg, iimm/ireg", (rm,args,state) -> {
            if(state.breg[args[0].getInt()]){
                switch (args[1].argumentType) {
                    case IREG -> state.ip += state.ireg[args[1].getInt()];
                    case IIMM -> state.ip += args[1].getInt();
                }
            }
        }),

        jump_long_if_true("jmplt breg, iimm/ireg", (rm,args,state) -> {
            if(state.breg[args[0].getInt()]){
                switch (args[1].argumentType) {
                    case IREG -> state.ip = state.ireg[args[1].getInt()];
                    case IIMM -> state.ip = args[1].getInt();
                }
            }
        }),

        push_registers("pushregs", (rm,args,state) -> {
            //push all registers
            for(int i=0;i<State.REGISTER_COUNT;i++) {
                state.istack.push(state.ireg[i]);
                state.bstack.push(state.breg[i]);
                state.sstack.push(state.sreg[i]);
                state.fstack.push(state.freg[i]);
            }

        }),

        pop_registers("popregs", (rm,args,state) -> {
            //pop all backwards
            for (int i = State.REGISTER_COUNT - 1; i >= 0; i--){
                state.freg[i] = state.fstack.pop();
                state.breg[i] = state.bstack.pop();
                state.sreg[i] = state.sstack.pop();
                state.ireg[i] = state.istack.pop();
            }

        }),

        reset_registers("resetregs", (rm,args,state) -> {
            for(int i=0;i<State.REGISTER_COUNT;i++){
                state.freg[i] = 0.0f;
                state.breg[i] = true;
                state.sreg[i] = new FiniteString("");
                state.ireg[i] = 0;
                state.oreg[i] = new Object();
//                state.ereg[i] = ErrorState.NONE;
            }
        }),

        reset_stacks("resetstacks", (rm,args,state) -> {
            state.istack.clear();
            state.fstack.clear();
            state.bstack.clear();
            state.sstack.clear();
            state.ostack.clear();
//            state.estack.clear();
        }),

        //=====START INTEGER INSTRUCTIONS=====

        //arithmetic instructions
        int_add("iadd ireg, ireg/iimm, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();
            final int arg2 = args[2].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[2].getInt()] : args[2].getInt();

            state.ireg[args[0].getInt()] = arg1 + arg2;
        }),
        int_sub("isub ireg, ireg/iimm, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();
            final int arg2 = args[2].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[2].getInt()] : args[2].getInt();

            state.ireg[args[0].getInt()] = arg1 - arg2;
        }),
        int_mul("imul ireg, ireg/iimm, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();
            final int arg2 = args[2].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[2].getInt()] : args[2].getInt();

            state.ireg[args[0].getInt()] = arg1 * arg2;
        }),
        int_div("idiv ireg, ireg/iimm, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();
            final int arg2 = args[2].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[2].getInt()] : args[2].getInt();

            if(arg2 == 0)
                throw new InterruptException(Interrupt.INT_DIV_BY_ZERO);

            state.ireg[args[0].getInt()] = arg1 / arg2;
        }),

        int_mod("imod ireg, ireg/iimm, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();
            final int arg2 = args[2].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[2].getInt()] : args[2].getInt();

            if(arg2 == 0)
                throw new InterruptException(Interrupt.INT_DIV_BY_ZERO);

            state.ireg[args[0].getInt()] = Math.floorMod(arg1, arg2);
        }),

        //conditionals
        int_gt("igt breg, ireg/iimm, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();
            final int arg2 = args[2].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[2].getInt()] : args[2].getInt();

            state.breg[args[0].getInt()] = arg1 > arg2;
        }),
        int_st("ist breg, ireg/iimm, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();
            final int arg2 = args[2].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[2].getInt()] : args[2].getInt();

            state.breg[args[0].getInt()] = arg1 < arg2;
        }),
        int_eq("ieq breg, ireg/iimm, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();
            final int arg2 = args[2].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[2].getInt()] : args[2].getInt();

            state.breg[args[0].getInt()] = arg1 == arg2;
        }),

        //value moving
        int_mov("imov ireg, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();

            state.ireg[args[0].getInt()] = arg1;
        }),
        int_load("iload ireg, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();

            state.ireg[args[0].getInt()] = state.imem.get(arg1);
        }),
        int_store("istore ireg, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();

            state.imem.set(state.ireg[args[0].getInt()], arg1);
        }),

        //stack instructions
        int_pop("ipop ireg", (rm,args,state) -> {
            state.ireg[args[0].getInt()] = state.istack.pop();
        }),
        int_push("ipush ireg/iimm", (rm,args,state) -> {
            final int arg0 = args[0].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[0].getInt()] : args[0].getInt();

            state.istack.push(arg0);
        }),
        //=====END INTEGER INSTRUCTIONS=====


        //=====START FLOAT INSTRUCTIONS=====

        //arithmetic instructions
        float_add("fadd freg, freg/fimm, freg/fimm", (rm,args,state) -> {
            final float arg1 = args[1].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[1].getInt()] : args[1].getFloat();
            final float arg2 = args[2].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[2].getInt()] : args[2].getFloat();

            state.freg[args[0].getInt()] = arg1 + arg2;
        }),
        float_sub("fsub freg, freg/fimm, freg/fimm", (rm,args,state) -> {
            final float arg1 = args[1].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[1].getInt()] : args[1].getFloat();
            final float arg2 = args[2].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[2].getInt()] : args[2].getFloat();

            state.freg[args[0].getInt()] = arg1 - arg2;
        }),
        float_mul("fmul freg, freg/fimm, freg/fimm", (rm,args,state) -> {
            final float arg1 = args[1].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[1].getInt()] : args[1].getFloat();
            final float arg2 = args[2].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[2].getInt()] : args[2].getFloat();

            state.freg[args[0].getInt()] = arg1 * arg2;
        }),
        float_div("fdiv freg, freg/fimm, freg/fimm", (rm,args,state) -> {
            final float arg1 = args[1].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[1].getInt()] : args[1].getFloat();
            final float arg2 = args[2].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[2].getInt()] : args[2].getFloat();

            if(arg2 == 0)
                throw new InterruptException(Interrupt.FLOAT_DIV_BY_ZERO);

            state.freg[args[0].getInt()] = arg1 / arg2;
        }),

        float_mod("fmod freg, freg/fimm, freg/fimm", (rm,args,state) -> {
            final float arg1 = args[1].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[1].getInt()] : args[1].getFloat();
            final float arg2 = args[2].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[2].getInt()] : args[2].getFloat();

            if(arg2 == 0)
                throw new InterruptException(Interrupt.INT_DIV_BY_ZERO);

            state.freg[args[0].getInt()] = arg1 % arg2;
        }),

        //conditionals
        float_gt("fgt breg, freg/fimm, freg/fimm", (rm,args,state) -> {
            final float arg1 = args[1].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[1].getInt()] : args[1].getFloat();
            final float arg2 = args[2].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[2].getInt()] : args[2].getFloat();

            state.breg[args[0].getInt()] = arg1 > arg2;
        }),
        float_st("fst breg, freg/fimm, freg/fimm", (rm,args,state) -> {
            final float arg1 = args[1].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[1].getInt()] : args[1].getFloat();
            final float arg2 = args[2].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[2].getInt()] : args[2].getFloat();

            state.breg[args[0].getInt()] = arg1 < arg2;
        }),
        float_eq("feq breg, freg/fimm, freg/fimm", (rm,args,state) -> {
            final float arg1 = args[1].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[1].getInt()] : args[1].getFloat();
            final float arg2 = args[2].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[2].getInt()] : args[2].getFloat();

            state.breg[args[0].getInt()] = arg1 == arg2;
        }),

        //value moving
        float_mov("fmov freg, freg/fimm", (rm,args,state) -> {
            final float arg1 = args[1].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[1].getInt()] : args[1].getFloat();

            state.freg[args[0].getInt()] = arg1;
        }),
        float_load("fload freg, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();

            state.freg[args[0].getInt()] = state.fmem.get(arg1);
        }),
        float_store("fstore ireg, freg/fimm", (rm,args,state) -> {
            final float arg1 = args[1].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[1].getInt()] : args[1].getFloat();

            state.fmem.set(state.ireg[args[0].getInt()], arg1);
        }),

        //stack instructions
        float_pop("fpop freg", (rm,args,state) -> {
            state.freg[args[0].getInt()] = state.fstack.pop();
        }),
        float_push("fpush freg/fimm", (rm,args,state) -> {
            final float arg0 = args[0].argumentType == OpContext.ArgumentType.FREG ?
                    state.freg[args[0].getInt()] : args[0].getFloat();

            state.fstack.push(arg0);
        }),
        //=====END FLOAT INSTRUCTIONS=====

        //=====START BOOLEAN INSTRUCTIONS=====
        //standard logic operations
        bool_and("and breg, breg/bimm, breg/bimm", (rm,args,state) -> {
            final boolean arg1 = args[1].argumentType == OpContext.ArgumentType.BREG ?
                    state.breg[args[1].getInt()] : args[1].getBool();
            final boolean arg2 = args[2].argumentType == OpContext.ArgumentType.BREG ?
                    state.breg[args[2].getInt()] : args[2].getBool();

            state.breg[args[0].getInt()] = arg1 && arg2;
        }),
        bool_or("or breg, breg/bimm, breg/bimm", (rm,args,state) -> {
            final boolean arg1 = args[1].argumentType == OpContext.ArgumentType.BREG ?
                    state.breg[args[1].getInt()] : args[1].getBool();
            final boolean arg2 = args[2].argumentType == OpContext.ArgumentType.BREG ?
                    state.breg[args[2].getInt()] : args[2].getBool();

            state.breg[args[0].getInt()] = arg1 || arg2;
        }),
        bool_xor("xor breg, breg/bimm, breg/bimm", (rm,args,state) -> {
            final boolean arg1 = args[1].argumentType == OpContext.ArgumentType.BREG ?
                    state.breg[args[1].getInt()] : args[1].getBool();
            final boolean arg2 = args[2].argumentType == OpContext.ArgumentType.BREG ?
                    state.breg[args[2].getInt()] : args[2].getBool();

            state.breg[args[0].getInt()] = arg1 ^ arg2;
        }),
        bool_not("not breg, breg/bimm", (rm,args,state) -> {
            final boolean arg1 = args[1].argumentType == OpContext.ArgumentType.BREG ?
                    state.breg[args[1].getInt()] : args[1].getBool();

            state.breg[args[0].getInt()] = !arg1;
        }),
//        bool_eq("eq breg, breg, breg", (rm,args,state) -> {
//            state.breg[args[0].getInt()] = state.breg[args[1].getInt()] == state.breg[args[2].getInt()];
//        }),

        //value moving
        bool_mov("bmov breg, breg/bimm", (rm,args,state) -> {
            final boolean arg1 = args[1].argumentType == OpContext.ArgumentType.BREG ?
                    state.breg[args[1].getInt()] : args[1].getBool();

            state.breg[args[0].getInt()] = arg1;
        }),
        bool_load("bload breg, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType == OpContext.ArgumentType.IREG ?
                    state.ireg[args[1].getInt()] : args[1].getInt();

            state.breg[args[0].getInt()] = state.bmem.get(arg1);
        }),
        bool_store("bstore ireg, breg/bimm", (rm,args,state) -> {
            final boolean arg1 = args[1].argumentType == OpContext.ArgumentType.BREG ?
                    state.breg[args[1].getInt()] : args[1].getBool();

            state.bmem.set(state.ireg[args[0].getInt()], arg1);
        }),

        //stack instructions
        bool_pop("bpop breg", (rm,args,state) -> {
            state.breg[args[0].getInt()] = state.bstack.pop();
        }),
        bool_push("bpush breg/bimm", (rm,args,state) -> {
            final boolean arg0 = args[0].argumentType == OpContext.ArgumentType.BREG ?
                    state.breg[args[0].getInt()] : args[0].getBool();

            state.bstack.push(arg0);
        }),
        //=====END BOOLEAN INSTRUCTIONS=====



        //=====START STRING INSTRUCTIONS=====
        //string stuff
        string_add("sadd sreg, sreg/simm, sreg/simm", (rm,args,state) -> {
            final String arg1 = args[1].argumentType== OpContext.ArgumentType.SREG ?
                    state.sreg[1].get() : args[1].getString();
            final String arg2 = args[2].argumentType== OpContext.ArgumentType.SREG ?
                    state.sreg[2].get() : args[2].getString();

            state.sreg[args[0].getInt()] = new FiniteString(arg1 + arg2);
        }),
        string_eq("seq breg, sreg/simm, sreg/simm", (rm,args,state) -> {
            final String arg1 = args[1].argumentType== OpContext.ArgumentType.SREG ?
                    state.sreg[1].get() : args[1].getString();
            final String arg2 = args[2].argumentType== OpContext.ArgumentType.SREG ?
                    state.sreg[2].get() : args[2].getString();

            state.breg[args[0].getInt()] = Objects.equals(arg1, arg2);
        }),

        //value moving
        string_mov("smov sreg, sreg/simm", (rm,args,state) -> {
            final String arg1 = args[1].argumentType== OpContext.ArgumentType.SREG ?
                    state.sreg[1].get() : args[1].getString();

            state.sreg[args[0].getInt()] = new FiniteString(arg1);
        }),
        string_load("sload sreg, ireg/iimm", (rm,args,state) -> {
            final int arg1 = args[1].argumentType== OpContext.ArgumentType.IREG ?
                    state.ireg[1] : args[1].getInt();

            state.sreg[args[0].getInt()] = new FiniteString(state.smem.get(arg1));
        }),
        string_store("sstore ireg, sreg/simm", (rm,args,state) -> {
            final String arg1 = args[1].argumentType== OpContext.ArgumentType.SREG ?
                    state.sreg[1].get() : args[1].getString();

            state.smem.set(state.ireg[args[0].getInt()], arg1);
        }),

        //stack instructions
        string_pop("spop sreg", (rm,args,state) -> {
            state.sreg[args[0].getInt()] = state.sstack.pop();
        }),
        string_push("spush sreg/simm", (rm,args,state) -> {
            final String arg0 = args[0].argumentType== OpContext.ArgumentType.SREG ?
                    state.sreg[0].get() : args[0].getString();

            state.sstack.push(new FiniteString(arg0));
        }),
        //=====END STRING INSTRUCTIONS=====


        //=====START OBJECT INSTRUCTIONS=====
        object_move("omov oreg, oreg", (rm,args,state) -> {
            final int arg0 = args[0].getInt();
            final int arg1 = args[1].getInt();
            state.oreg[arg0] = state.oreg[arg1];
        }),
        object_push("opush oreg", (rm,args,state) -> {
            final int arg0 = args[0].getInt();

            state.ostack.push(state.oreg[arg0]);
        }),
        // opop is an anagram for poop
        object_pop("opop oreg", (rm,args,state) -> {
            final int arg0 = args[0].getInt();

            state.oreg[arg0] = state.ostack.pop();
        }),
        //=====END OBJECT INSTRUCTIONS=====
        ;

        public static final Map<String, SpecLanguage.Parser.Argument[]> specInstructionMap = new HashMap<>();
        public static final Map<String, Instruction> opcodeMap = new HashMap<>();
        public static final Map<Instruction, String> opcodeMapB = new HashMap<>();

        public final String spec;

        private interface InstructionConsumer{
            void accept(RegisterMachine rm, OpContext.Argument[] args, State state)
                    throws InterruptException;
        }
        public final InstructionConsumer exec;
        public final SpecLanguage.Parser.ParseData parseData;
        Instruction(String spec, InstructionConsumer exec){
            this.spec = spec;
            this.exec = exec;

            final List<SpecLanguage.Lexer.Token> tokens = SpecLanguage.Lexer.lex(spec);
            this.parseData = SpecLanguage.Parser.parse(tokens);
        }

//        @Override
//        public String toString() {
//            return "Instruction{" +
//                    "spec='" + spec + '\'' +
//                    '}';
//        }
    }



    static{
        for(Instruction instruction : EnumSet.allOf(Instruction.class)){
            List<SpecLanguage.Lexer.Token> lexTokens = SpecLanguage.Lexer.lex(instruction.spec);
            SpecLanguage.Parser.ParseData parseData = SpecLanguage.Parser.parse(lexTokens);

            Instruction.opcodeMap.put(parseData.OPCODE, instruction);
            Instruction.opcodeMapB.put(instruction, parseData.OPCODE);
            Instruction.specInstructionMap.put(parseData.OPCODE, parseData.args);
        }
    }

    public static class SpecLanguage{
        public static class Lexer{
            enum TokenType{KEYWORD}
            static class Token{
                public final String[] value;
                public final TokenType type;

                public Token(String[] value, TokenType type) {
                    this.value = value;
                    this.type = type;
                }

                @Override
                public String
                toString() {
                    return "Token{" +
                            "value='" + Arrays.toString(value) + '\'' +
                            ", type=" + type +
                            '}';
                }
        }

            enum State{WAITING, KEYWORD, SLASH, ERROR}
            public static List<Token> lex(String input){
                List<Token> tokens = new ArrayList<>();

                StringBuilder buffer = new StringBuilder();
                List<String> values = new ArrayList<>();

                State state = State.WAITING;
                for(char c : (input+" ").toCharArray()){
                    if(state == State.WAITING){
                        if(Character.isAlphabetic(c)) state = State.KEYWORD;
                        else if(Character.isWhitespace(c) || c==',')continue;
                        else state = State.ERROR;

                        buffer = new StringBuilder();
                        buffer.append(c);
                    }else if(state == State.KEYWORD){
                        if(c == '/') {
                            values.add(buffer.toString());
                            buffer = new StringBuilder();
                            state = State.SLASH;
                        }
                        else if(Character.isLetterOrDigit(c) || c == '_')
                            buffer.append(c);
                        else if(Character.isWhitespace(c) || c == ','){
                            state = State.WAITING;
                            values.add(buffer.toString());
                            tokens.add(new Token(values.toArray(new String[0]), TokenType.KEYWORD));
                            values.clear();
                        }else{
                            state = State.ERROR;
                        }
                    }else if(state == State.SLASH){
                        if(Character.isAlphabetic(c)){
                            buffer.append(c);
                            state = State.KEYWORD;
                        }else{
                            state = State.ERROR;
                        }
                    }
                }
                return tokens;
            }
        }

        public static class Parser{
            public enum ArgumentType{
                INTEGER_REG, BOOL_REG, FLOAT_REG, STRING_REG, OBJECT_REG,
                INTEGER_IMM, BOOL_IMM, FLOAT_IMM, STRING_IMM,
                ;
                public OpContext.ArgumentType toOpContext(){
                    return toOpContext(this);
                }
                public static OpContext.ArgumentType toOpContext(ArgumentType argType){
                    switch(argType){
                        case BOOL_IMM:return OpContext.ArgumentType.BIMM;
                        case BOOL_REG:return OpContext.ArgumentType.BREG;
                        case FLOAT_IMM:return OpContext.ArgumentType.FIMM;
                        case FLOAT_REG:return OpContext.ArgumentType.FREG;
                        case INTEGER_IMM:return OpContext.ArgumentType.IIMM;
                        case INTEGER_REG:return OpContext.ArgumentType.IREG;
                        case OBJECT_REG:return OpContext.ArgumentType.OREG;
                        case STRING_IMM:return OpContext.ArgumentType.SIMM;
                        case STRING_REG:return OpContext.ArgumentType.SREG;
//                        case ERROR_REG:return OpContext.ArgumentType.EREG;
                        default: return null;
                    }
                }
            }
            static class Argument{
                //possible argument types
                final ArgumentType[] argumentType;
//                final Object value;

                public Argument(ArgumentType[] argumentType) {
                    this.argumentType = argumentType;
//                    this.value = value;
                }

                @Override
                public String toString() {
                    return "Argument{" +
                            "argumentType=" + Arrays.toString(argumentType) +
//                            ", value=" + value +
                            '}';
                }

            }
            public static class ParseData{
                final String OPCODE;
                final Argument[] args;

                public ParseData(String OPCODE, Argument[] args) {
                    this.OPCODE = OPCODE;
                    this.args = args;
                }

                @Override
                public String toString() {
                    return "ParseData{" +
                            "OPCODE='" + OPCODE + '\'' +
                            ", args=" + Arrays.toString(args) +
                            '}';
                }
            }

            public static ParseData parse(List<Lexer.Token> tokens){
                final String OPCODE = tokens.get(0).value[0].toLowerCase();

                List<ArgumentType> argumentTypes = new ArrayList<>();
                List<Argument> args = new ArrayList<>();
                for(int i=1;i<tokens.size();i++){
                    final Lexer.Token token = tokens.get(i);
                    for(String value : token.value){
                        if(value.toLowerCase().endsWith("reg")){
                            final char start = value.toLowerCase().charAt(0);
                            switch (start) {
                                case 'i' -> argumentTypes.add(ArgumentType.INTEGER_REG);
                                case 'f' -> argumentTypes.add(ArgumentType.FLOAT_REG);
                                case 'b' -> argumentTypes.add(ArgumentType.BOOL_REG);
                                case 's' -> argumentTypes.add(ArgumentType.STRING_REG);
                                case 'o' -> argumentTypes.add(ArgumentType.OBJECT_REG);

//                                case 'e':argumentTypes.add(ArgumentType.ERROR_REG);break;
                            }
                        }else if(value.toLowerCase().endsWith("imm")){
                            final char start = value.toLowerCase().charAt(0);
                            switch (start) {
                                case 'i' -> argumentTypes.add(ArgumentType.INTEGER_IMM);
                                case 'f' -> argumentTypes.add(ArgumentType.FLOAT_IMM);
                                case 'b' -> argumentTypes.add(ArgumentType.BOOL_IMM);
                                case 's' -> argumentTypes.add(ArgumentType.STRING_IMM);
                            }
                        }else{
                            //ERROR
                        }
                    }
                    args.add(new Argument(argumentTypes.toArray(new ArgumentType[0])));
                    argumentTypes.clear();
                }
                return new ParseData(OPCODE, args.toArray(new Argument[0]));
            }
        }
    }

//    TODO(code easy): add macros. predefined syscall macros can be made
    public static class Assembler{
        public static class Lexer{
            enum TokenType{KEYWORD, INT_LIT, FLOAT_LIT, STRING_LIT, LABEL}

            public record Token(String value, TokenType type) {
                @Override
                public String toString() {
                    return "Token{" +
                            "value='" + value + '\'' +
                            ", type=" + type +
                            '}';
                }
            }
            enum State{WAITING, KEYWORD, STRING_LITERAL, SIGN, BUILDING_NUMBER,
                INT_LITERAL, FLOAT_LITERAL1, FLOAT_LITERAL2, LABEL, ERROR, COMMENT}
            public static List<Token> lex(String input){
                List<Token> tokens = new ArrayList<>();

                State state = State.WAITING;
                StringBuilder buffer = new StringBuilder();
                for(char c : (input + " ").toCharArray()){
                    if(c == ';'){
                        state = State.COMMENT;
                    }else if(state == State.COMMENT){
                        if(c == '\n')
                            state = State.WAITING;
                    }else if(state == State.WAITING) {
                        if (c == '"') state = State.STRING_LITERAL;
                        else if(c == ':') state = State.LABEL;
                        else if (c == '+' || c == '-') state = State.SIGN;
                        else if (Character.isAlphabetic(c)) state = State.KEYWORD;
                        else if (Character.isDigit(c)) state = State.BUILDING_NUMBER;
                        else if (Character.isWhitespace(c) || c == '\n') continue;
                        else state = State.ERROR;

                        buffer = new StringBuilder();
                        buffer.append(c);
                    }else if(state == State.STRING_LITERAL) {
                        //TODO(code easy): allow escaped characters
                        buffer.append(c);
                        if (c == '"') {
                            tokens.add(new Token(buffer.toString(), TokenType.STRING_LIT));
                            state = State.WAITING;
                        }
                    }else if(state == State.LABEL){
                        if(Character.isLetterOrDigit(c) || "_.".contains(String.valueOf(c))){
                            buffer.append(c);
                        }else if(Character.isWhitespace(c) || c == '\n'){
                            tokens.add(new Token(buffer.substring(1, buffer.length()),
                                    TokenType.LABEL));
                            state = State.WAITING;
                        }else{
                            state = State.ERROR;
                        }
                    }else if(state == State.SIGN){
                        if(Character.isDigit(c)){
                            state = State.BUILDING_NUMBER;
                            buffer.append(c);
                        }else{
                            state = State.ERROR;
                        }
                    }else if(state == State.BUILDING_NUMBER){
                        if(Character.isDigit(c)){
                            buffer.append(c);
                        }else if(c == '.'){
                            state = State.FLOAT_LITERAL1;
                            buffer.append(c);
                        }else if(Character.isWhitespace(c)){
                            state = State.WAITING;
                            tokens.add(new Token(buffer.toString(), TokenType.INT_LIT));
                        }else{
                            state = State.ERROR;
                        }
                    }else if(state == State.FLOAT_LITERAL1){
                        if(Character.isDigit(c)){
                            state = State.FLOAT_LITERAL2;
                            buffer.append(c);
                        }else{
                            state = State.ERROR;
                        }
                    }else if(state == State.FLOAT_LITERAL2){
                        if(Character.isDigit(c)){
                            buffer.append(c);
                        }else if(Character.isWhitespace(c) || c == '\n'){
                            tokens.add(new Token(buffer.toString(), TokenType.FLOAT_LIT));
                            state = State.WAITING;
                        }else{
                            state = State.ERROR;
                        }
                    }else if(state == State.KEYWORD){
                        if(Character.isLetterOrDigit(c)){
                            buffer.append(c);
                        }else if(Character.isWhitespace(c)){
                            tokens.add(new Token(buffer.toString(), TokenType.KEYWORD));
                            state = State.WAITING;
                        }else{
                            state = State.ERROR;
                        }
                    }

                }
                return tokens;
            }
        }

        public static class CompleteInstruction {
            public final Instruction instruction;
            public OpContext.Argument[] arguments;

            public CompleteInstruction(Instruction instruction, OpContext.Argument[] arguments) {
                this.instruction = instruction;
                this.arguments = arguments;
            }

            public CompleteInstruction clone(){
                if(this.arguments == null){
                    return new CompleteInstruction(this.instruction, null);
                }
                OpContext.Argument[] arguments = new OpContext.Argument[this.arguments.length];
                for (int i = 0; i < this.arguments.length; i++) {
                    final OpContext.Argument arg = this.arguments[i];
                    arguments[i] = arg.clone();
                }
                return new CompleteInstruction(this.instruction, arguments);
            }

            @Override
            public String toString() {
                return "CompleteInstruction{" +
                        "instruction=" + instruction.name() +
                        ", arguments=" + Arrays.toString(arguments) +
                        '}';
            }

            public String prettyString(){
                StringBuilder builder= new StringBuilder(instruction.name());

                if(arguments != null)
                    for(OpContext.Argument arg : arguments)
                        builder.append(String.format(" (%s|%s), ", arg.argumentType.name(), arg.value));

//                builder.append("\n");
                return builder.toString();
            }
        }

        public static class Parser{
            public static class WrongTypeException extends Exception{
                public WrongTypeException(String message) {
                    super(message);
                }
            }

            enum State{WAITING, ERROR, ARGUMENTS}
            public static List<CompleteInstruction> parse(List<Lexer.Token> tokens) throws WrongTypeException {
                List<CompleteInstruction> instructions = new ArrayList<>();
                State state = State.WAITING;

                Map<String, Integer> labelMap = new HashMap<>();

                SpecLanguage.Parser.Argument[] specArgTypes = null;
                OpContext.Argument[] argsBuffer = null;

                String currentOP = null;
                int argi = 0;

                for(Lexer.Token token : tokens){
//                    System.out.printf("state: %s, token: %s, buffer: %s,\n",
//                            state.name(), token, Arrays.toString(argsBuffer));
                    if(state == State.WAITING){
                        if(token.type == Lexer.TokenType.KEYWORD){
                            currentOP = token.value;
                            argi = 0;
                            specArgTypes = Instruction.specInstructionMap.get(currentOP);
                            argsBuffer = new OpContext.Argument[specArgTypes.length];

                            if(specArgTypes.length == 0){//check if it takes zero args
                                final Instruction instruction = Instruction.opcodeMap.get(currentOP.toLowerCase());

                                instructions.add(new CompleteInstruction(instruction, argsBuffer));
                            }else{//else we are expecting next tokens to be arguments
                                state = State.ARGUMENTS;
                            }
//                            System.out.printf("argsBuffer length: %d\n", argsBuffer.length);
                        }else if(token.type == Lexer.TokenType.LABEL){
                            labelMap.put(token.value, instructions.size());
                        }else{
                            state = State.ERROR;
                        }
                    }else if(state == State.ARGUMENTS){
                        if(token.type == Lexer.TokenType.LABEL){
                            argsBuffer[argi] = new OpContext.Argument(
                                    OpContext.ArgumentType.UNRESOLVED_LABEL, token.value);
                        }else if(token.type == Lexer.TokenType.FLOAT_LIT){
                            argsBuffer[argi] = new OpContext.Argument(
                                    OpContext.ArgumentType.FIMM, Float.valueOf(token.value));
                        }else if(token.type == Lexer.TokenType.INT_LIT){
                            argsBuffer[argi] = new OpContext.Argument(
                                    OpContext.ArgumentType.IIMM, Integer.valueOf(token.value));
                        }else if(token.type == Lexer.TokenType.STRING_LIT){
                            argsBuffer[argi] = new OpContext.Argument(
                                    OpContext.ArgumentType.SIMM, token.value.substring(1,token.value.length()-1));
                        }else if(token.type == Lexer.TokenType.KEYWORD){//can only be a register now, like ireg0
                            OpContext.ArgumentType type = switch (token.value.toLowerCase().charAt(0)) {
                                case 'i' -> OpContext.ArgumentType.IREG;
                                case 'f' -> OpContext.ArgumentType.FREG;
                                case 's' -> OpContext.ArgumentType.SREG;
                                case 'o' -> OpContext.ArgumentType.OREG;
                                case 'b' -> OpContext.ArgumentType.BREG;
                                default -> null;
//                                case 'e': type=OpContext.ArgumentType.EREG;break;
                            };
                            int regValue = Integer.parseInt(token.value.substring(4));

                            argsBuffer[argi] = new OpContext.Argument(type, regValue);
                        }

                        final SpecLanguage.Parser.ArgumentType[] expectedArgTypes
                                = specArgTypes[argi].argumentType;

                        final SpecLanguage.Parser.ArgumentType currentType
                                = argsBuffer[argi].argumentType.toSpecLang();

                        if(!List.of(expectedArgTypes).contains(currentType)){
                            final String message = String.format(
                                    "Type Error| op: %s, arg index: %d, expected types: %s, got %s (%s) instead.",
                                    currentOP, argi, Arrays.toString(expectedArgTypes),
                                    argsBuffer[argi].value.toString(), currentType.name()
                            );
                            throw new WrongTypeException(message);
                        }

                        argi++;
                        if(argi == argsBuffer.length){
                            state = State.WAITING;

                            final Instruction instruction = Instruction.opcodeMap.get(currentOP.toLowerCase());

                            instructions.add(new CompleteInstruction(instruction, argsBuffer));
                        }
                    }
                }

                for (final CompleteInstruction instruction : instructions) {
                    for (int j = 0; j < instruction.arguments.length; j++) {
                        final OpContext.Argument arg = instruction.arguments[j];

                        if (arg.argumentType == OpContext.ArgumentType.UNRESOLVED_LABEL) {
                            final int labelIp = labelMap.get((String) arg.value);
                            instruction.arguments[j] = new OpContext.Argument(OpContext.ArgumentType.IIMM, labelIp-1);
                        }
                    }

                }

                return instructions;
            }
        }

        public static List<CompleteInstruction> assemble(final String program) throws Parser.WrongTypeException {
            List<Assembler.Lexer.Token> asmTokens = Assembler.Lexer.lex(program);
//            System.out.println(program);

            return Parser.parse(asmTokens);
        }
    }

    static class BinaryRepresentation{
        public final InstructionBlock instructionBlock;
        public final DataBlock dataBlock;
        public BinaryRepresentation(InstructionBlock instructionBlock, DataBlock dataBlock) {
            this.instructionBlock = instructionBlock;
            this.dataBlock = dataBlock;
        }

        private enum ReadingState {LOOKING_BLOCK, RESOLVE_IMMEDIATES, FINISHED}
        private enum WritingState {ERROR}//TODO: this is unused rn
        public static void writeToOutputStream(List<Assembler.CompleteInstruction> program, OutputStream output)
                throws IOException {
            WritingState state = WritingState.ERROR;

            byte[] buffer = new byte[4];
            ByteBuffer wrapper = ByteBuffer.wrap(buffer);

            //write magic header for instructiton block (1 byte)
            output.write(InstructionBlock.MAGIC_HEADER);

            wrapper.putShort((short)program.size());
            output.write(buffer, 0 , 2); // write # of instructions (2 bytes)
            wrapper.clear();

            for(Assembler.CompleteInstruction instruction : program){
                //write opcode (2 bytes)
                wrapper.clear();
                wrapper.putShort((short)instruction.instruction.ordinal());
                output.write(buffer, 0, 2);

                //write argument length (1 byte)
                wrapper.clear();
                wrapper.put((byte)instruction.arguments.length);
                output.write(buffer, 0, 1);

                //write arg descriptors
                for(OpContext.Argument argument : instruction.arguments){
                    //write argtype (1 byte)
                    output.write((byte)argument.argumentType.ordinal());

                    //write arg size (4 bytes)
                    wrapper.clear();
                    wrapper.putInt(argument.getSize());
                    output.write(buffer, 0, 4);
                }

                //write arguments
                for(OpContext.Argument argument : instruction.arguments){
                    //write arg (? bytes)
//                    System.out.println("writing arg " + argument);
                    switch (argument.argumentType) {
                        case BIMM -> {
                            if (argument.getBool()) output.write(1);
                            else output.write(0);
                        }
                        case FIMM -> {
                            wrapper.clear();
                            wrapper.putFloat(argument.getFloat());
                            output.write(buffer, 0, 4);
                        }
                        case SIMM -> {
                            final String stringVal = argument.getString();
                            output.write(stringVal.getBytes(), 0, stringVal.length());
                        }
                        default -> {
                            wrapper.clear();
                            wrapper.putInt(argument.getInt());
                            output.write(buffer, 0, 4);
                        }
                    }

                }

            }
        }

        public static byte[] toByteArray(List<Assembler.CompleteInstruction> program)  {
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

                BinaryRepresentation.writeToOutputStream(program, byteStream);

                return byteStream.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        public static BinaryRepresentation fromInputStream(InputStream input) throws IOException {
            InstructionBlock instructionBlock1 = null;
            DataBlock dataBlock1 = null;

            ReadingState state = ReadingState.LOOKING_BLOCK;
            int data;
            while(state != ReadingState.FINISHED){ //lol?
                data = input.read();
                if(state == ReadingState.LOOKING_BLOCK){
                    if(instructionBlock1 != null){
                        state = ReadingState.FINISHED;
                    }else{
                        switch(data){
                            case InstructionBlock.MAGIC_HEADER:
//                                System.out.println("reading instruction block...");
                                instructionBlock1 = InstructionBlock.fromInputStream(input);break;
                            case DataBlock.MAGIC_HEADER:
//                                System.out.println("reading datta block...");
                                dataBlock1 = DataBlock.fromInputStream(input);break;
                        }
                    }

                }else if(state == ReadingState.RESOLVE_IMMEDIATES){

                }
            }
            return new BinaryRepresentation(instructionBlock1, dataBlock1);
        }

        public static BinaryRepresentation fromBytes(byte[] bytes){
            try {
                return fromInputStream(new ByteArrayInputStream(bytes));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        record InstructionBlock(Assembler.CompleteInstruction[] completeInstructions) {
            public static final byte MAGIC_HEADER = 1;

            private enum State {
                INSTRUCTION_LENGTH,
                READING_INSTRUCTIONS,
                FINISHED,
            }

            public static InstructionBlock fromInputStream(InputStream input) throws IOException {
                State state = State.INSTRUCTION_LENGTH;

                List<Assembler.CompleteInstruction> completeInstructionList = new ArrayList<>();

                int instructions_length = 0;
                int instructionIndex = 0;

                byte[] buffer = new byte[4];
                ByteBuffer bufferWrapper = ByteBuffer.wrap(buffer);

                while (state != State.FINISHED) {
                    if (state == State.INSTRUCTION_LENGTH) {
                        input.readNBytes(buffer, 0, 2); //read 2 bytes
                        instructions_length = bufferWrapper.getShort(0);
//                        System.out.println("instruction length == " + instructions_length);
                        state = State.READING_INSTRUCTIONS;
                    } else if (state == State.READING_INSTRUCTIONS) {
                        if (instructionIndex > instructions_length - 1) {
                            state = State.FINISHED;
                        } else {
                            //read the opcode (2 bytes)
                            input.readNBytes(buffer, 0, 2);
//                            System.out.println(Arrays.toString(buffer));

                            final short opcode = bufferWrapper.getShort(0);
                            final Instruction instruction =
                                    BinaryRepresentation.instructionFromOpcode(opcode);

//                            System.out.println("2opcode " + instruction.name());
//                            System.out.println();

                            //read argument length (1 byte)
                            input.readNBytes(buffer, 0, 1);
//                            System.out.println(Arrays.toString(buffer));

                            final byte arguments = bufferWrapper.get(0);
                            OpContext.ArgumentType[] argTypes = new OpContext.ArgumentType[arguments];
                            int[] argSizes = new int[arguments];

//                            System.out.println("1argument length " + arguments);
//                            System.out.println();

                            //reading argument types
                            for (int argi = 0; argi < arguments; argi++) {
                                //read arg type (1 byte)
                                input.readNBytes(buffer, 0, 1);
//                                System.out.println(Arrays.toString(buffer));

                                final byte argByteData = bufferWrapper.get(0);
                                //read arg size (4 bytes)
                                input.readNBytes(buffer, 0, 4);
//                                System.out.println(Arrays.toString(buffer));

                                final int argByteLength = bufferWrapper.getInt(0);

//                                System.out.println("1arg type " + argByteData);
//                                System.out.println("4arg length " + argByteLength);
//                                System.out.println();

                                final OpContext.ArgumentType argType =
                                        BinaryRepresentation.argTypeFromByte(argByteData);

                                argTypes[argi] = argType;
                                argSizes[argi] = argByteLength;
                            }

                            OpContext.Argument[] opContextArguments = new OpContext.Argument[arguments];

                            //reading the arguments
                            for (int argi = 0; argi < arguments; argi++) {
                                final int argLength = argSizes[argi];
                                final OpContext.ArgumentType argType = argTypes[argi];
                                Object value;

//                                System.out.printf("reading %d bytes for argument\n", argLength);

                                if (argType == OpContext.ArgumentType.SIMM) {
                                    value = new String(input.readNBytes(argLength));
//                                    System.out.println(Arrays.toString(buffer));
//
//                                    System.out.printf("read string: '%s'\n\n", value);
                                } else {
                                    input.readNBytes(buffer, 0, argLength);
//                                    System.out.println(Arrays.toString(buffer));
//
//                                    System.out.printf("%dread a register, bimm or fimm...\n", argLength);
                                    switch (argType) {
                                        case BIMM:
                                            value = bufferWrapper.getInt(0) == 1;
                                            break;
                                        case FIMM:
                                            value = bufferWrapper.getFloat(0);
                                            break;
                                        default:
                                            value = bufferWrapper.getInt(0);
                                            break;
                                    }
                                }
                                opContextArguments[argi] = new OpContext.Argument(argType, value);
                            }

                            final Assembler.CompleteInstruction completeInstruction =
                                    new Assembler.CompleteInstruction(instruction, opContextArguments);

                            completeInstructionList.add(completeInstruction);

                            instructionIndex++;
                        }
                    }
                }

                final Assembler.CompleteInstruction[] instructions =
                        completeInstructionList.toArray(new Assembler.CompleteInstruction[0]);

                return new InstructionBlock(instructions);
            }
        }

        private static OpContext.ArgumentType argTypeFromByte(byte argByteData) {
            return OpContext.ArgumentType.values()[argByteData];
        }

        private static Instruction instructionFromOpcode(short opcode) {
            return Instruction.values()[opcode];
        }

        static class DataBlock{
            public static final byte MAGIC_HEADER = 2;
            public static DataBlock fromInputStream(InputStream input) throws IOException {
                return null;
            }
        }
    }

    public static void main(String[] args) {
        String assemblerInput =
                "jmpl :_start\n" +
                "\n" +
                ":doStuff_entry\n" +
                "call \"sys.println\"\n" +
                "ret\n" +
                "\n" +
                "\n" +
                ":_start\n" +
                "; init the procedures\n" +
                "registercall \"doStuff\" :doStuff_entry\n" +
                "\n" +
                "smov sreg0 \"hello sigma\"\n" +
                "call \"doStuff\"\n" +
                "halt\n" +
                "call \"doStuff\""
                ;
        List<Assembler.Lexer.Token> asmTokens = Assembler.Lexer.lex(assemblerInput);
        System.out.println(assemblerInput);
//        System.out.println(asmTokens);

        try {
            List<Assembler.CompleteInstruction> instructions = Assembler.Parser.parse(asmTokens);

            final byte[] rawBytes = BinaryRepresentation.toByteArray(instructions);

            System.out.printf("bytes: %s, array size : %d\n",
                    Arrays.toString(rawBytes), rawBytes.length);

//            System.out.println(instructions);

            Assembler.CompleteInstruction[] moreInstructions =
                    BinaryRepresentation.fromBytes(rawBytes)
                            .instructionBlock.completeInstructions;

//            System.out.println(Arrays.toString(moreInstructions));

            RegisterMachine registerMachine = new RegisterMachine();

            registerMachine.resetStateAndLoadProgram(List.of(moreInstructions));

//            System.out.println("before, ireg: " + Arrays.toString(registerMachine.state.ireg));

            registerMachine.stepUntilHalt();

            System.out.println("result iregs: " + Arrays.toString(registerMachine.state.ireg));

        } catch (Assembler.Parser.WrongTypeException e) {
            System.err.println(e.getMessage());
        } catch (InterruptException e) {
            throw new RuntimeException(e);
        }

//        String specLangInput = "jmp iimm/ireg";
//        System.out.println(specLangInput);
//
//        List<SpecLanguage.Lexer.Token> specTokens = SpecLanguage.Lexer.lex(specLangInput);
//        System.out.println(specTokens);
//
//        SpecLanguage.Parser.ParseData parseData = SpecLanguage.Parser.parse(specTokens);
//        System.out.println(parseData);
    }

}
