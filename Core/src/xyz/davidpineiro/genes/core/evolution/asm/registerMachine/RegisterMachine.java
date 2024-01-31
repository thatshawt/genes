package xyz.davidpineiro.genes.core.evolution.asm.registerMachine;

import org.apache.commons.lang.NullArgumentException;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RegisterMachine {

    protected State state = new State();

    public void step() {
        final Assembler.CompleteInstruction instruction = state.program[state.ip];

        OpContext context = new OpContext(this, instruction.arguments);

        try {
            instruction.instruction.exec.accept(context.arguments, state);
            state.ip++;
        } catch (InterruptException e) {
            final int i = e.interrupt.i;
            if(state.interrupt_mask_table[i])
                state.ip = state.interrupt_handler_table[i];
        }
    }

    public void stepUntilHalt(){
        while(state.ip < state.program.length || state.halt){
            step();
        }
    }

    public void resetStateAndLoadProgram(List<Assembler.CompleteInstruction> instructions){
        state = new State();
        state.program = instructions.toArray(new Assembler.CompleteInstruction[0]);
    }

    private static class MachineMemory<T>{
        private final SortedMap<Integer, T> memory = new TreeMap<>();
        private final Supplier<T> defaultValue;

        public MachineMemory(Supplier<T> defaultValue) {
            this.defaultValue = defaultValue;
        }

        public void set(int address, T val){
            memory.put(address, val);
        }

        public T get(int address){
            if(!memory.containsKey(address)) set(address, defaultValue.get());
            return memory.get(address);
        }
    }

    private static class StringMachineMemory extends MachineMemory<String>{
        public StringMachineMemory() {
            super(() -> "");
        }
    }
    private static class IntMachineMemory extends MachineMemory<Integer>{
        public IntMachineMemory() {
            super(() -> 0);
        }
    }
    private static class FloatMachineMemory extends MachineMemory<Float>{
        public FloatMachineMemory() {
            super(() -> 0.0f);
        }
    }
    private static class BooleanMachineMemory extends MachineMemory<Boolean>{
        public BooleanMachineMemory() {
            super(() -> true);
        }
    }

    private static class MachineStack<T>{
        private Stack<T> stack = new Stack<>();

        public T pop() throws InterruptException {
            if (stack.size() == 0)
                throw new InterruptException(Interrupt.POP_EMPTY_STACK);

            return stack.pop();
        }

        //works the same, throws excpetion when you try to push null value
        public void push(T val){
            if(val == null)
                throw new NullArgumentException("bad!!!");

            stack.push(val);
        }

        public void clear(){
            stack.clear();
        }
    }

    protected static class InterruptException extends Exception{
        public final Interrupt interrupt;
        public InterruptException(Interrupt interrupt) {
            super(interrupt.name());
            this.interrupt = interrupt;
        }
    }

    public enum Interrupt{
        //exceptions
        INT_DIV_BY_ZERO(0),
        FLOAT_DIV_BY_ZERO(1),
        POP_EMPTY_STACK(2),

        BAD_SYSCALL_ARGUMENTS(3),
        ;

        public final int i;
//        public final boolean enabled;
        Interrupt(int i){
            this.i = i;
//            this.enabled = enabled;
        }

    }

    public enum SystemCall{
        PRINT(0, (state) -> {
            System.out.print(state.sreg[0]);
        }),

        ;

        public final int i;
        public final Consumer<State> exec;
        SystemCall(int i, Consumer<State> exec){
            this.i = i;
            this.exec = exec;
        }

        public static final Map<Integer, Consumer<State>> sysCallMap = new HashMap<>();

        static{
            for(SystemCall systemCall : SystemCall.values())
                sysCallMap.put(systemCall.i, systemCall.exec);
        }

    }

    protected static class State{
        State(){
            Arrays.fill(interrupt_mask_table, false);
        }
        protected int ip = 0;
        protected boolean halt = false;

        protected Assembler.CompleteInstruction[] program;

        public static final int REGISTER_COUNT = 5;

        protected int[] interrupt_handler_table = new int[Interrupt.values().length];
        protected boolean[] interrupt_mask_table = new boolean[Interrupt.values().length];

        protected int[] ireg = new int[REGISTER_COUNT];
        protected boolean[] breg = new boolean[REGISTER_COUNT];
        protected float[] freg = new float[REGISTER_COUNT];
        protected String[] sreg = new String[REGISTER_COUNT];
        protected Object[] oreg = new Object[REGISTER_COUNT];
//        protected ErrorState[] ereg = new ErrorState[REGISTER_COUNT];


        protected IntMachineMemory imem = new IntMachineMemory();
        protected BooleanMachineMemory bmem = new BooleanMachineMemory();
        protected FloatMachineMemory fmem = new FloatMachineMemory();
        protected StringMachineMemory smem = new StringMachineMemory();
        protected MachineMemory<Object> omem = new MachineMemory<>(Object::new);


        protected MachineStack<Integer> istack = new MachineStack<>();
        protected MachineStack<Boolean> bstack = new MachineStack<>();
        protected MachineStack<Float> fstack = new MachineStack<>();
        protected MachineStack<String> sstack = new MachineStack<>();
        protected MachineStack<Object> ostack = new MachineStack<>();
//        public MachineStack<ErrorState> estack = new MachineStack<>();

    }

    private static class OpContext {
        enum ArgumentType{//9 different ones
            IREG, BREG, FREG, SREG, OREG,
            IIMM, BIMM, FIMM, SIMM,
            ;

            public SpecLanguage.Parser.ArgumentType toSpecLang(){
                switch (this){
                    case SIMM: return SpecLanguage.Parser.ArgumentType.STRING_IMM;
                    case SREG: return SpecLanguage.Parser.ArgumentType.STRING_REG;
                    case BIMM: return SpecLanguage.Parser.ArgumentType.BOOL_IMM;
                    case BREG : return SpecLanguage.Parser.ArgumentType.BOOL_REG;
                    case FIMM : return SpecLanguage.Parser.ArgumentType.FLOAT_IMM;
                    case FREG : return SpecLanguage.Parser.ArgumentType.FLOAT_REG;
                    case IIMM : return SpecLanguage.Parser.ArgumentType.INTEGER_IMM;
                    case IREG : return SpecLanguage.Parser.ArgumentType.INTEGER_REG;
                    case OREG : return SpecLanguage.Parser.ArgumentType.OBJECT_REG;
//                    case EREG : return SpecLanguage.Parser.ArgumentType.ERROR_REG;
                    default: return null;
                }
            }

        }
        static class Argument{
            public final ArgumentType argumentType;
            public final Object value;

            public Argument(ArgumentType argumentType, Object value) {
                this.argumentType = argumentType;
                this.value = value;
            }

            @Override
            public String toString() {
                return "Argument{" +
                        "argumentType=" + argumentType +
                        ", value=" + value +
                        '}';
            }

            public int getInt(){
                return (int)value;
            }
            public boolean getBool(){
                return (boolean)value;
            }
            public String getString(){
                return (String)value;
            }
            public float getFloat(){
                return (float)value;
            }
            public Object getObj(){
                return value;
            }

            public int getSize() {
                switch(argumentType){
                    case SIMM: return getString().length();
                    case BIMM: return 1;
                    default: return 4;
                }
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
        nop("nop", (args,state) -> {}),

        halt("halt", (args,state) -> {
            state.halt = true;
        }),

        //TODO: should interrupts call an interrupt if the interruptI is out of bounds?
        //interrupt instructions
        interrupt_set("int_set ireg/iimm ireg/iimm", (args,state) -> {
            final int interruptI;
            if(args[0].argumentType == OpContext.ArgumentType.IREG){
                interruptI = state.ireg[args[0].getInt()];
            }else {
                interruptI = args[0].getInt();
            }

            final int ip;
            if(args[1].argumentType == OpContext.ArgumentType.IREG){
                ip = state.ireg[args[1].getInt()];
            }else {
                ip = args[1].getInt();
            }

            if(interruptI >= 0 && interruptI < state.interrupt_handler_table.length)
                state.interrupt_handler_table[interruptI] = ip;
        }),

        interrupt_enable("int_enable ireg/iimm", (args,state)->{
            final int interruptI;
            if(args[0].argumentType == OpContext.ArgumentType.IREG){
                interruptI = state.ireg[args[0].getInt()];
            }else {
                interruptI = args[0].getInt();
            }

            if(interruptI >= 0 && interruptI < state.interrupt_handler_table.length)
                state.interrupt_mask_table[interruptI] = true;
        }),

        interrupt_disable("int_disable ireg/iimm", (args,state)->{
            final int interruptI;
            if(args[0].argumentType == OpContext.ArgumentType.IREG){
                interruptI = state.ireg[args[0].getInt()];
            }else {
                interruptI = args[0].getInt();
            }

            if(interruptI >= 0 && interruptI < state.interrupt_handler_table.length)
                state.interrupt_mask_table[interruptI] = false;
        }),

        syscall("syscall ireg/iimm", (args,state) -> {
            final int syscallI;
            if(args[0].argumentType == OpContext.ArgumentType.IREG){
                syscallI = state.ireg[args[0].getInt()];
            }else {
                syscallI = args[0].getInt();
            }
            if(SystemCall.sysCallMap.containsKey(syscallI))
                SystemCall.sysCallMap.get(syscallI).accept(state);
        }),

        ip_load("ipload ireg", (args,state) -> {
            state.ireg[args[0].getInt()] = state.ip;
        }),

        ip_push("ippush", (args,state) -> {
            state.istack.push(state.ip);
        }),

        //branching instructions
        jump("jmp iimm/ireg", (args,state) -> {
            if(args[0].argumentType == OpContext.ArgumentType.IREG){
                state.ip += state.ireg[args[0].getInt()];
            }else if(args[0].argumentType == OpContext.ArgumentType.IIMM){
                state.ip += args[0].getInt();
            }
        }),

        jump_long("jmpl iimm/ireg", (args,state) -> {
            switch(args[0].argumentType){
                case IREG:
                    state.ip = state.ireg[args[0].getInt()];
                    break;
                case IIMM:
                    state.ip = args[0].getInt();
                    break;
            }
        }),

        jump_if_true("jmpt breg, iimm/ireg", (args,state) -> {
            if(state.breg[args[0].getInt()]){
                switch(args[1].argumentType){
                    case IREG:
                        state.ip += state.ireg[args[1].getInt()];
                        break;
                    case IIMM:
                        state.ip += args[1].getInt();
                        break;
                }
            }
        }),

        jump_long_if_true("jmplt breg, iimm/ireg", (args,state) -> {
            if(state.breg[args[0].getInt()]){
                switch(args[1].argumentType){
                    case IREG:
                        state.ip = state.ireg[args[1].getInt()];
                        break;
                    case IIMM:
                        state.ip = args[1].getInt();
                        break;
                }
            }
        }),

        push_registers("pushregs", (args,state) -> {
            //push all registers
            for(int i=0;i<State.REGISTER_COUNT;i++) {
                state.istack.push(state.ireg[i]);
                state.bstack.push(state.breg[i]);
                state.sstack.push(state.sreg[i]);
                state.fstack.push(state.freg[i]);
            }

        }),

        pop_registers("popregs", (args,state) -> {
            //pop all backwards
            for (int i = State.REGISTER_COUNT - 1; i >= 0; i--){
                state.freg[i] = state.fstack.pop();
                state.breg[i] = state.bstack.pop();
                state.sreg[i] = state.sstack.pop();
                state.ireg[i] = state.istack.pop();
            }

        }),

        reset_registers("resetregs", (args,state) -> {
            for(int i=0;i<State.REGISTER_COUNT;i++){
                state.freg[i] = 0.0f;
                state.breg[i] = true;
                state.sreg[i] = "";
                state.ireg[i] = 0;
                state.oreg[i] = new Object();
//                state.ereg[i] = ErrorState.NONE;
            }
        }),

        reset_stacks("resetstacks", (args,state) -> {
            state.istack.clear();
            state.fstack.clear();
            state.bstack.clear();
            state.sstack.clear();
            state.ostack.clear();
//            state.estack.clear();
        }),

        //=====START INTEGER INSTRUCTIONS=====

        //arithmetic instructions
        int_add("iadd ireg, ireg, ireg", (args,state) -> {
            state.ireg[args[0].getInt()] = state.ireg[args[1].getInt()] + state.ireg[args[2].getInt()];
        }),
        int_sub("isub ireg, ireg, ireg", (args,state) -> {
            state.ireg[args[0].getInt()] = state.ireg[args[1].getInt()] - state.ireg[args[2].getInt()];
        }),
        int_mul("imul ireg, ireg, ireg", (args,state) -> {
            state.ireg[args[0].getInt()] = state.ireg[args[1].getInt()] * state.ireg[args[2].getInt()];
        }),
        int_div("idiv ireg, ireg, ireg", (args,state) -> {
            if(args[2].getInt() == 0)
                throw new InterruptException(Interrupt.INT_DIV_BY_ZERO);
            state.ireg[args[0].getInt()] = state.ireg[args[1].getInt()] / state.ireg[args[2].getInt()];
        }),

        int_mod("imod ireg, ireg, ireg", (args,state) -> {
            state.ireg[args[0].getInt()] = state.ireg[args[1].getInt()] % state.ireg[args[2].getInt()];
        }),

        //conditionals
        int_gt("igt breg, ireg, ireg", (args,state) -> {
            state.breg[args[0].getInt()] = state.ireg[args[1].getInt()] > state.ireg[args[2].getInt()];
        }),
        int_st("ist breg, ireg, ireg", (args,state) -> {
            state.breg[args[0].getInt()] = state.ireg[args[1].getInt()] < state.ireg[args[2].getInt()];
        }),
        int_eq("ieq breg, ireg, ireg", (args,state) -> {
            state.breg[args[0].getInt()] = state.ireg[args[1].getInt()] == state.ireg[args[2].getInt()];
        }),

        //value moving
        int_mov("imov ireg, iimm", (args,state) -> {
            state.ireg[args[0].getInt()] = args[1].getInt();
        }),
        int_load("iload ireg, ireg", (args,state) -> {
            state.ireg[args[0].getInt()] = state.imem.get(state.ireg[args[1].getInt()]);
        }),
        int_store("istore ireg, ireg", (args,state) -> {
            state.imem.set(state.ireg[args[0].getInt()], state.ireg[args[1].getInt()]);
        }),

        //stack instructions
        int_pop("ipop ireg", (args,state) -> {
            state.ireg[args[0].getInt()] = state.istack.pop();
        }),
        int_push("ipush ireg", (args,state) -> {
            state.istack.push(state.ireg[args[0].getInt()]);
        }),
        //=====END INTEGER INSTRUCTIONS=====


        //=====START FLOAT INSTRUCTIONS=====

        //arithmetic instructions
        float_add("fadd freg, freg, freg", (args,state) -> {
            state.freg[args[0].getInt()] = state.freg[args[1].getInt()] + state.freg[args[2].getInt()];
        }),
        float_sub("fsub freg, freg, freg", (args,state) -> {
            state.freg[args[0].getInt()] = state.freg[args[1].getInt()] - state.freg[args[2].getInt()];
        }),
        float_mul("fmul freg, freg, freg", (args,state) -> {
            state.freg[args[0].getInt()] = state.freg[args[1].getInt()] * state.freg[args[2].getInt()];
        }),
        float_div("fdiv freg, freg, freg", (args,state) -> {
            if(args[2].getInt() == 0)
                throw new InterruptException(Interrupt.FLOAT_DIV_BY_ZERO);
            state.freg[args[0].getInt()] = state.freg[args[1].getInt()] / state.freg[args[2].getInt()];
        }),

        float_mod("fmod freg, freg, freg", (args,state) -> {
            state.freg[args[0].getInt()] = state.freg[args[1].getInt()] % state.freg[args[2].getInt()];
        }),

        //conditionals
        float_gt("fgt breg, freg, freg", (args,state) -> {
            state.breg[args[0].getInt()] = state.freg[args[1].getInt()] > state.freg[args[2].getInt()];
        }),
        float_st("fst breg, freg, freg", (args,state) -> {
            state.breg[args[0].getInt()] = state.freg[args[1].getInt()] < state.freg[args[2].getInt()];
        }),
        float_eq("feq breg, freg, freg", (args,state) -> {
            state.breg[args[0].getInt()] = state.freg[args[1].getInt()] == state.freg[args[2].getInt()];
        }),

        //value moving
        float_mov("fmov freg, fimm", (args,state) -> {
            state.freg[args[0].getInt()] = args[1].getFloat();
        }),
        float_load("fload freg, ireg", (args,state) -> {
            state.freg[args[0].getInt()] = state.fmem.get(state.ireg[args[1].getInt()]);
        }),
        float_store("fstore ireg, freg", (args,state) -> {
            state.fmem.set(state.ireg[args[0].getInt()], state.freg[args[1].getInt()]);
        }),

        //stack instructions
        float_pop("fpop freg", (args,state) -> {
            state.freg[args[0].getInt()] = state.fstack.pop();
        }),
        float_push("fpush freg", (args,state) -> {
            state.fstack.push(state.freg[args[0].getInt()]);
        }),
        //=====END FLOAT INSTRUCTIONS=====

        //=====START BOOLEAN INSTRUCTIONS=====
        //standard logic operations
        bool_and("and breg, breg, breg", (args,state) -> {
            state.breg[args[0].getInt()] = state.breg[args[1].getInt()] && state.breg[args[2].getInt()];
        }),
        bool_or("or breg, breg, breg", (args,state) -> {
            state.breg[args[0].getInt()] = state.breg[args[1].getInt()] || state.breg[args[2].getInt()];
        }),
        bool_xor("xor breg, breg, breg", (args,state) -> {
            state.breg[args[0].getInt()] = state.breg[args[1].getInt()] ^ state.breg[args[2].getInt()];
        }),
        bool_not("not breg, breg", (args,state) -> {
            state.breg[args[0].getInt()] = !state.breg[args[1].getInt()];
        }),
//        bool_eq("eq breg, breg, breg", (args,state) -> {
//            state.breg[args[0].getInt()] = state.breg[args[1].getInt()] == state.breg[args[2].getInt()];
//        }),

        //value moving
        bool_mov("bmov breg, bimm", (args,state) -> {
            state.breg[args[0].getInt()] = args[1].getBool();
        }),
        bool_load("bload breg, ireg", (args,state) -> {
            state.breg[args[0].getInt()] = state.bmem.get(state.ireg[args[1].getInt()]);
        }),
        bool_store("bstore ireg, breg", (args,state) -> {
            state.bmem.set(state.ireg[args[0].getInt()], state.breg[args[1].getInt()]);
        }),

        //stack instructions
        bool_pop("bpop breg", (args,state) -> {
            state.breg[args[0].getInt()] = state.bstack.pop();
        }),
        bool_push("bpush breg", (args,state) -> {
            state.bstack.push(state.breg[args[0].getInt()]);
        }),
        //=====END BOOLEAN INSTRUCTIONS=====



        //=====START STRING INSTRUCTIONS=====
        //string stuff
        string_add("sadd sreg, sreg, sreg", (args,state) -> {
            state.sreg[args[0].getInt()] = state.sreg[args[1].getInt()] + state.sreg[args[2].getInt()];
        }),
        string_eq("seq breg, sreg, sreg", (args,state) -> {
            state.breg[args[0].getInt()] = Objects.equals(state.sreg[args[1].getInt()], state.sreg[args[2].getInt()]);
        }),

        //value moving
        string_mov("smov sreg, simm", (args,state) -> {
            state.sreg[args[0].getInt()] = args[1].getString();
        }),
        string_load("sload sreg, ireg", (args,state) -> {
            state.sreg[args[0].getInt()] = state.smem.get(state.ireg[args[1].getInt()]);
        }),
        string_store("sstore ireg, sreg", (args,state) -> {
            state.smem.set(state.ireg[args[0].getInt()], state.sreg[args[1].getInt()]);
        }),

        //stack instructions
        string_pop("spop sreg", (args,state) -> {
            state.sreg[args[0].getInt()] = state.sstack.pop();
        }),
        string_push("spush sreg", (args,state) -> {
            state.sstack.push(state.sreg[args[0].getInt()]);
        }),
        //=====END STRING INSTRUCTIONS=====


        //=====START OBJECT INSTRUCTIONS=====
        //what to do with object instructions ;-;
        //=====END OBJECT INSTRUCTIONS=====
        ;

        public static final Map<String, SpecLanguage.Parser.Argument[]> specInstructionMap = new HashMap<>();
        public static final Map<String, Instruction> opcodeMap = new HashMap<>();

        public final String spec;

        private interface InstructionConsumer{
            void accept(OpContext.Argument[] args, State state) throws InterruptException;
        }
        public final InstructionConsumer exec;
        public final SpecLanguage.Parser.ParseData parseData;
        Instruction(String spec, InstructionConsumer exec){
            this.spec = spec;
            this.exec = exec;

            final List<SpecLanguage.Lexer.Token> tokens = SpecLanguage.Lexer.lex(spec);
            this.parseData = SpecLanguage.Parser.parse(tokens);
        }
    }

    static{
        for(Instruction instruction : EnumSet.allOf(Instruction.class)){
            List<SpecLanguage.Lexer.Token> lexTokens = SpecLanguage.Lexer.lex(instruction.spec);
            SpecLanguage.Parser.ParseData parseData = SpecLanguage.Parser.parse(lexTokens);

            Instruction.opcodeMap.put(parseData.OPCODE, instruction);
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
                            switch(start){
                                case 'i':argumentTypes.add(ArgumentType.INTEGER_REG);break;
                                case 'f':argumentTypes.add(ArgumentType.FLOAT_REG);break;
                                case 'b':argumentTypes.add(ArgumentType.BOOL_REG);break;
                                case 's':argumentTypes.add(ArgumentType.STRING_REG);break;
                                case 'o':argumentTypes.add(ArgumentType.OBJECT_REG);break;
//                                case 'e':argumentTypes.add(ArgumentType.ERROR_REG);break;
                            }
                        }else if(value.toLowerCase().endsWith("imm")){
                            final char start = value.toLowerCase().charAt(0);
                            switch(start){
                                case 'i':argumentTypes.add(ArgumentType.INTEGER_IMM);break;
                                case 'f':argumentTypes.add(ArgumentType.FLOAT_IMM);break;
                                case 'b':argumentTypes.add(ArgumentType.BOOL_IMM);break;
                                case 's':argumentTypes.add(ArgumentType.STRING_IMM);break;
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

    public static class Assembler{
        public static class Lexer{
            enum TokenType{KEYWORD, INT_LIT, FLOAT_LIT, STRING_LIT}
            static class Token{
                public final String value;
                public final TokenType type;

                public Token(String value, TokenType type) {
                    this.value = value;
                    this.type = type;
                }

                @Override
                public String
                toString() {
                    return "Token{" +
                            "value='" + value + '\'' +
                            ", type=" + type +
                            '}';
                }
            }
            enum State{WAITING, KEYWORD, STRING_LITERAL, SIGN, BUILDING_NUMBER,
                INT_LITERAL, FLOAT_LITERAL1, FLOAT_LITERAL2, ERROR}
            public static List<Token> lex(String input){
                List<Token> tokens = new ArrayList<>();

                State state = State.WAITING;
                StringBuilder buffer = new StringBuilder();
                for(char c : (input + " ").toCharArray()){
                    if(state == State.WAITING){
                        if(c == '"')state = State.STRING_LITERAL;
                        else if(c == '+' || c == '-')state = State.SIGN;
                        else if(Character.isAlphabetic(c))state = State.KEYWORD;
                        else if(Character.isDigit(c))state = State.BUILDING_NUMBER;
                        else if(Character.isWhitespace(c) || c == ';' || c == '\n')continue;
                        else state = State.ERROR;

                        buffer = new StringBuilder();
                        buffer.append(c);
                    }else if(state == State.STRING_LITERAL){
                        buffer.append(c);
                        if(c == '"'){
                            tokens.add(new Token(buffer.toString(), TokenType.STRING_LIT));
                            state = State.WAITING;
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
                        }else if(Character.isWhitespace(c) || c == ';' || c == '\n'){
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
            public final OpContext.Argument[] arguments;

            public CompleteInstruction(Instruction instruction, OpContext.Argument[] arguments) {
                this.instruction = instruction;
                this.arguments = arguments;
            }

            @Override
            public String toString() {
                return "CompleteInstruction{" +
                        "instruction=" + instruction +
                        ", arguments=" + Arrays.toString(arguments) +
                        '}';
            }
        }

        public static class Parser{
            /**
             * thrown when a type is not what it should be
             */
            static class WrongTypeException extends Exception{
                public WrongTypeException(String message) {
                    super(message);
                }
            }

            enum State{WAITING, ERROR, ARGUMENTS}
            public static List<CompleteInstruction> parse(List<Lexer.Token> tokens) throws WrongTypeException {
                List<CompleteInstruction> instructions = new ArrayList<>();
                State state = State.WAITING;

                SpecLanguage.Parser.Argument[] specArgTypes = null;
                OpContext.Argument[] argsBuffer = null;

                String currentOP = null;
                int argi = 0;

                for(Lexer.Token token : tokens){
                    if(state == State.WAITING){
                        if(token.type == Lexer.TokenType.KEYWORD){
                            currentOP = token.value;
                            argi = 0;
                            specArgTypes = Instruction.specInstructionMap.get(currentOP);
                            argsBuffer = new OpContext.Argument[specArgTypes.length];
                            state = State.ARGUMENTS;
//                            System.out.printf("argsBuffer length: %d\n", argsBuffer.length);
                        }else{
                            state = State.ERROR;
                        }
                    }else if(state == State.ARGUMENTS){

                        if(token.type == Lexer.TokenType.FLOAT_LIT){
                            argsBuffer[argi] = new OpContext.Argument(
                                    OpContext.ArgumentType.FIMM, Float.valueOf(token.value));
                        }else if(token.type == Lexer.TokenType.INT_LIT){
                            argsBuffer[argi] = new OpContext.Argument(
                                    OpContext.ArgumentType.IIMM, Integer.valueOf(token.value));
                        }else if(token.type == Lexer.TokenType.STRING_LIT){
                            argsBuffer[argi] = new OpContext.Argument(
                                    OpContext.ArgumentType.SIMM, token.value.substring(1,token.value.length()-1));
                        }else if(token.type == Lexer.TokenType.KEYWORD){//can only be a register now, like ireg0
                            OpContext.ArgumentType type = null;
                            switch(token.value.toLowerCase().charAt(0)){
                                case 'i': type=OpContext.ArgumentType.IREG;break;
                                case 'f': type=OpContext.ArgumentType.FREG;break;
                                case 's': type=OpContext.ArgumentType.SREG;break;
                                case 'o': type=OpContext.ArgumentType.OREG;break;
                                case 'b': type=OpContext.ArgumentType.BREG;break;
//                                case 'e': type=OpContext.ArgumentType.EREG;break;
                            }
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

                return instructions;
            }
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
        private enum WritingState {ERROR}
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
                    switch(argument.argumentType){
                        case BIMM:
                            if (argument.getBool()) output.write(1);
                             else output.write(0);
                            break;
                        case FIMM:
                            wrapper.clear();
                            wrapper.putFloat(argument.getFloat());
                            output.write(buffer, 0, 4);
                            break;
                        case SIMM:
                            final String stringVal = argument.getString();
                            output.write(stringVal.getBytes(), 0, stringVal.length());
                            break;
                        default:
                            wrapper.clear();
                            wrapper.putInt(argument.getInt());
                            output.write(buffer, 0, 4);
                            break;
                    }

                }

            }
        }

        public static byte[] toByteArray(List<Assembler.CompleteInstruction> program)  {
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

                BinaryRepresentation.writeToOutputStream(program, byteStream);

                final byte[] rawBytes = byteStream.toByteArray();
                return rawBytes;
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

        static class InstructionBlock{
            public static final byte MAGIC_HEADER = 1;

            private enum State{
                INSTRUCTION_LENGTH,
                READING_INSTRUCTIONS,
                FINISHED,
            }

            public final Assembler.CompleteInstruction[] completeInstructions;

            public InstructionBlock(Assembler.CompleteInstruction[] completeInstructions) {
                this.completeInstructions = completeInstructions;
            }

            public static InstructionBlock fromInputStream(InputStream input) throws IOException {
                State state = State.INSTRUCTION_LENGTH;

                List<Assembler.CompleteInstruction> completeInstructionList = new ArrayList<>();

                int instructions_length = 0;
                int instructionIndex = 0;

                byte[] buffer = new byte[4];
                ByteBuffer bufferWrapper = ByteBuffer.wrap(buffer);

                while(state != State.FINISHED){
                    if(state == State.INSTRUCTION_LENGTH){
                        input.readNBytes(buffer, 0, 2); //read 2 bytes
                        instructions_length = bufferWrapper.getShort(0);
//                        System.out.println("instruction length == " + instructions_length);
                        state = State.READING_INSTRUCTIONS;
                    }else if(state == State.READING_INSTRUCTIONS){
                        if(instructionIndex > instructions_length-1){
                            state = State.FINISHED;
                        }else{
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
                            for(int argi=0;argi<arguments;argi++){
                                //read arg type (1 byte)
                                input.readNBytes(buffer, 0, 1);
//                                System.out.println(Arrays.toString(buffer));

                                final byte argByteData =  bufferWrapper.get(0);
                                //read arg size (4 bytes)
                                input.readNBytes(buffer, 0, 4);
//                                System.out.println(Arrays.toString(buffer));

                                final int argByteLength =  bufferWrapper.getInt(0);

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
                            for(int argi=0;argi<arguments;argi++){
                                final int argLength = argSizes[argi];
                                final OpContext.ArgumentType argType = argTypes[argi];
                                Object value;

//                                System.out.printf("reading %d bytes for argument\n", argLength);

                                if(argType == OpContext.ArgumentType.SIMM){
                                    value = new String(input.readNBytes(argLength));
//                                    System.out.println(Arrays.toString(buffer));
//
//                                    System.out.printf("read string: '%s'\n\n", value);
                                }else{
                                    input.readNBytes(buffer, 0, argLength);
//                                    System.out.println(Arrays.toString(buffer));
//
//                                    System.out.printf("%dread a register, bimm or fimm...\n", argLength);
                                    switch(argType){
                                        case BIMM:value = bufferWrapper.getInt(0)==1;break;
                                        case FIMM:value = bufferWrapper.getFloat(0);break;
                                        default:value = bufferWrapper.getInt(0);break;
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
                "smov sreg0 \"hello world!\"\n" +
                "syscall 0\n"
                ;
        List<Assembler.Lexer.Token> asmTokens = Assembler.Lexer.lex(assemblerInput);
        System.out.println(assemblerInput);

        try {
            List<Assembler.CompleteInstruction> instructions = Assembler.Parser.parse(asmTokens);

            final byte[] rawBytes = BinaryRepresentation.toByteArray(instructions);

            System.out.printf("bytes: %s, array size : %d\n",
                    Arrays.toString(rawBytes), rawBytes.length);

//            System.out.println(instructions);
            Assembler.CompleteInstruction[] moreInstructions =
                    BinaryRepresentation.fromBytes(rawBytes)
                            .instructionBlock.completeInstructions;

            System.out.println(Arrays.toString(moreInstructions));

            RegisterMachine registerMachine = new RegisterMachine();

            registerMachine.resetStateAndLoadProgram(List.of(moreInstructions));

            System.out.println("before, ireg: " + Arrays.toString(registerMachine.state.ireg));

            registerMachine.stepUntilHalt();

            System.out.println("after, ireg: " + Arrays.toString(registerMachine.state.ireg));

        } catch (Assembler.Parser.WrongTypeException e) {
            System.err.println(e.getMessage());
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
