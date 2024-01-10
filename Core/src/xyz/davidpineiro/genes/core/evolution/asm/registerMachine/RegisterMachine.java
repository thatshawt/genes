package xyz.davidpineiro.genes.core.evolution.asm.registerMachine;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class RegisterMachine {

    protected State state = new State();

    public void step(){
        final Assembler.CompleteInstruction instruction = state.program[state.ip];

        OpContext context = new OpContext(this, instruction.arguments);

        instruction.instruction.exec.accept(context);
        state.ip++;
    }

    public void stepUntilHalt(){
        while(state.ip < state.program.length)step();
    }

    public void loadProgramAndResetIp(List<Assembler.CompleteInstruction> instructions){
        state.program = instructions.toArray(new Assembler.CompleteInstruction[0]);
        state.ip = 0;
    }

    protected static class State{
        protected int ip = 0;

        //TODO fix this ugly
        public Assembler.CompleteInstruction[] program;

        protected int[] ireg = new int[5];
        protected boolean[] breg = new boolean[5];
        protected float[] freg = new float[5];
        protected String[] sreg = new String[5];
        protected Object[] oreg = new Object[5];

        protected List<Integer> imem = new ArrayList<>();
        protected List<Boolean> bmem = new ArrayList<>();
        protected List<Float> fmem = new ArrayList<>();
        protected List<String> smem = new ArrayList<>();
        protected List<Object> omem = new ArrayList<>();

        protected Stack<Integer> istack =  new Stack<>();
        protected Stack<Boolean> bstack =  new Stack<>();
        protected Stack<Float> fstack =  new Stack<>();
        protected Stack<String> sstack =  new Stack<>();
        protected Stack<Object> ostack =  new Stack<>();
    }

    private static class OpContext {
        enum ArgumentType{//9 different ones
            IREG, BREG, FREG, SREG, OREG,
            IIMM, BIMM, FIMM, SIMM,
            ;
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
        //special instructions
        ip_load("ipload ireg", (c) -> {
            c.rm.state.ireg[c.arguments[0].getInt()] = c.rm.state.ip;
        }),
        jmp("jmp iimm/ireg", (c) -> {
            final OpContext.Argument arg0 = c.arguments[0];
            if(arg0.argumentType == OpContext.ArgumentType.IREG){
                c.rm.state.ip = c.rm.state.ireg[arg0.getInt()];
            }else if(arg0.argumentType == OpContext.ArgumentType.IIMM){
                c.rm.state.ip = arg0.getInt();
            }
        }),

        //=====START INTEGER INSTRUCTIONS=====

        //arithmetic instructions
        int_add("iadd ireg, ireg, ireg", (c) -> {
            final OpContext.Argument arg0 = c.arguments[0];
            final OpContext.Argument arg1 = c.arguments[1];
            final OpContext.Argument arg2 = c.arguments[2];
            c.rm.state.ireg[arg0.getInt()] = c.rm.state.ireg[arg1.getInt()] + c.rm.state.ireg[arg2.getInt()];
        }),
        int_sub("isub ireg, ireg, ireg", (c) -> {
            c.rm.state.ireg[c.arguments[0].getInt()] = c.rm.state.ireg[c.arguments[1].getInt()] - c.rm.state.ireg[c.arguments[2].getInt()];
        }),
        int_mul("imul ireg, ireg, ireg", (c) -> {
            c.rm.state.ireg[c.arguments[0].getInt()] = c.rm.state.ireg[c.arguments[1].getInt()] * c.rm.state.ireg[c.arguments[2].getInt()];
        }),
        int_div("idiv ireg, ireg, ireg", (c) -> {
            c.rm.state.ireg[c.arguments[0].getInt()] = c.rm.state.ireg[c.arguments[1].getInt()] / c.rm.state.ireg[c.arguments[2].getInt()];
        }),

        int_mod("imod ireg, ireg, ireg", (c) -> {
            c.rm.state.ireg[c.arguments[0].getInt()] = c.rm.state.ireg[c.arguments[1].getInt()] % c.rm.state.ireg[c.arguments[2].getInt()];
        }),

        //conditionals
        int_gt("igt breg, ireg, ireg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.ireg[c.arguments[1].getInt()] > c.rm.state.ireg[c.arguments[2].getInt()];
        }),
        int_st("ist breg, ireg, ireg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.ireg[c.arguments[1].getInt()] < c.rm.state.ireg[c.arguments[2].getInt()];
        }),
        int_eq("ieq breg, ireg, ireg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.ireg[c.arguments[1].getInt()] == c.rm.state.ireg[c.arguments[2].getInt()];
        }),

        //value moving
        int_mov("imov ireg, iimm", (c) -> {
            c.rm.state.ireg[c.arguments[0].getInt()] = c.arguments[1].getInt();
        }),
        int_load("iload ireg, ireg", (c) -> {
            c.rm.state.ireg[c.arguments[0].getInt()] = c.rm.state.imem.get(c.rm.state.ireg[c.arguments[1].getInt()]);
        }),
        int_store("istore ireg, ireg", (c) -> {
            c.rm.state.imem.set(c.rm.state.ireg[c.arguments[0].getInt()], c.rm.state.ireg[c.arguments[1].getInt()]);
        }),

        //stack instructions
        int_pop("ipop ireg", (c) -> {
            c.rm.state.ireg[c.arguments[0].getInt()] = c.rm.state.istack.pop();
        }),
        int_push("ipush ireg", (c) -> {
            c.rm.state.istack.push(c.rm.state.ireg[c.arguments[0].getInt()]);
        }),
        //=====END INTEGER INSTRUCTIONS=====


        //=====START FLOAT INSTRUCTIONS=====

        //arithmetic instructions
        float_add("fadd freg, freg, freg", (c) -> {
            c.rm.state.freg[c.arguments[0].getInt()] = c.rm.state.freg[c.arguments[1].getInt()] + c.rm.state.freg[c.arguments[2].getInt()];
        }),
        float_sub("fsub freg, freg, freg", (c) -> {
            c.rm.state.freg[c.arguments[0].getInt()] = c.rm.state.freg[c.arguments[1].getInt()] - c.rm.state.freg[c.arguments[2].getInt()];
        }),
        float_mul("fmul freg, freg, freg", (c) -> {
            c.rm.state.freg[c.arguments[0].getInt()] = c.rm.state.freg[c.arguments[1].getInt()] * c.rm.state.freg[c.arguments[2].getInt()];
        }),
        float_div("fdiv freg, freg, freg", (c) -> {
            c.rm.state.freg[c.arguments[0].getInt()] = c.rm.state.freg[c.arguments[1].getInt()] / c.rm.state.freg[c.arguments[2].getInt()];
        }),

        float_mod("fmod freg, freg, freg", (c) -> {
            c.rm.state.freg[c.arguments[0].getInt()] = c.rm.state.freg[c.arguments[1].getInt()] % c.rm.state.freg[c.arguments[2].getInt()];
        }),

        //conditionals
        float_gt("fgt breg, freg, freg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.freg[c.arguments[1].getInt()] > c.rm.state.freg[c.arguments[2].getInt()];
        }),
        float_st("fst breg, freg, freg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.freg[c.arguments[1].getInt()] < c.rm.state.freg[c.arguments[2].getInt()];
        }),
        float_eq("feq breg, freg, freg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.freg[c.arguments[1].getInt()] == c.rm.state.freg[c.arguments[2].getInt()];
        }),

        //value moving
        float_mov("fmov freg, fimm", (c) -> {
            c.rm.state.freg[c.arguments[0].getInt()] = c.arguments[1].getFloat();
        }),
        float_load("fload freg, ireg", (c) -> {
            c.rm.state.freg[c.arguments[0].getInt()] = c.rm.state.fmem.get(c.rm.state.ireg[c.arguments[1].getInt()]);
        }),
        float_store("fstore ireg, freg", (c) -> {
            c.rm.state.fmem.set(c.rm.state.ireg[c.arguments[0].getInt()], c.rm.state.freg[c.arguments[1].getInt()]);
        }),

        //stack instructions
        float_pop("fpop freg", (c) -> {
            c.rm.state.freg[c.arguments[0].getInt()] = c.rm.state.fstack.pop();
        }),
        float_push("fpush freg", (c) -> {
            c.rm.state.fstack.push(c.rm.state.freg[c.arguments[0].getInt()]);
        }),
        //=====END FLOAT INSTRUCTIONS=====

        //=====START BOOLEAN INSTRUCTIONS=====
        //standard logic operations
        bool_and("and breg, breg, breg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.breg[c.arguments[1].getInt()] && c.rm.state.breg[c.arguments[2].getInt()];
        }),
        bool_or("or breg, breg, breg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.breg[c.arguments[1].getInt()] || c.rm.state.breg[c.arguments[2].getInt()];
        }),
        bool_xor("xor breg, breg, breg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.breg[c.arguments[1].getInt()] ^ c.rm.state.breg[c.arguments[2].getInt()];
        }),
        bool_not("not breg, breg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = !c.rm.state.breg[c.arguments[1].getInt()];
        }),
//        bool_eq("eq breg, breg, breg", (c) -> {
//            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.breg[c.arguments[1].getInt()] == c.rm.state.breg[c.arguments[2].getInt()];
//        }),

        //value moving
        bool_mov("bmov breg, bimm", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.arguments[1].getBool();
        }),
        bool_load("bload breg, ireg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.bmem.get(c.rm.state.ireg[c.arguments[1].getInt()]);
        }),
        bool_store("bstore ireg, breg", (c) -> {
            c.rm.state.bmem.set(c.rm.state.ireg[c.arguments[0].getInt()], c.rm.state.breg[c.arguments[1].getInt()]);
        }),

        //stack instructions
        bool_pop("bpop breg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = c.rm.state.bstack.pop();
        }),
        bool_push("bpush breg", (c) -> {
            c.rm.state.bstack.push(c.rm.state.breg[c.arguments[0].getInt()]);
        }),
        //=====END BOOLEAN INSTRUCTIONS=====



        //=====START STRING INSTRUCTIONS=====
        //string stuff
        string_add("sadd sreg, sreg, sreg", (c) -> {
            c.rm.state.sreg[c.arguments[0].getInt()] = c.rm.state.sreg[c.arguments[1].getInt()] + c.rm.state.sreg[c.arguments[2].getInt()];
        }),
        string_eq("seq breg, sreg, sreg", (c) -> {
            c.rm.state.breg[c.arguments[0].getInt()] = Objects.equals(c.rm.state.sreg[c.arguments[1].getInt()], c.rm.state.sreg[c.arguments[2].getInt()]);
        }),

        //value moving
        string_mov("smov sreg, simm", (c) -> {
            c.rm.state.sreg[c.arguments[0].getInt()] = c.arguments[1].getString();
        }),
        string_load("sload sreg, ireg", (c) -> {
            c.rm.state.sreg[c.arguments[0].getInt()] = c.rm.state.smem.get(c.rm.state.ireg[c.arguments[1].getInt()]);
        }),
        string_store("sstore ireg, sreg", (c) -> {
            c.rm.state.smem.set(c.rm.state.ireg[c.arguments[0].getInt()], c.rm.state.sreg[c.arguments[1].getInt()]);
        }),

        //stack instructions
        string_pop("spop sreg", (c) -> {
            c.rm.state.sreg[c.arguments[0].getInt()] = c.rm.state.sstack.pop();
        }),
        string_push("spush sreg", (c) -> {
            c.rm.state.sstack.push(c.rm.state.sreg[c.arguments[0].getInt()]);
        }),
        //=====END STRING INSTRUCTIONS=====


        //=====START OBJECT INSTRUCTIONS=====
        //TODO: nothing i guess for now!?!??!?!?
        //=====END OBJECT INSTRUCTIONS=====
        ;

        public static final Map<String, SpecLanguage.Parser.Argument[]> specInstructionMap = new HashMap<>();
        public static final Map<String, Instruction> opcodeMap = new HashMap<>();

        public final String spec;
        public final Consumer<OpContext> exec;
        public final SpecLanguage.Parser.ParseData parseData;
        Instruction(String spec, Consumer<OpContext> exec){
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
            enum State{WAITING, ERROR, ARGUMENTS}
            public static List<CompleteInstruction> parse(List<Lexer.Token> tokens){
                List<CompleteInstruction> instructions = new ArrayList<>();
                State state = State.WAITING;

                SpecLanguage.Parser.Argument[] specArgTypes;

                OpContext.Argument[] argsBuffer = new OpContext.Argument[0];
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
                        //TODO raise some type of error if tthe types do not match
//                        final SpecLanguage.Parser.Argument expectedArg = specArgTypes[argi];

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
                            }
                            int regValue = Integer.parseInt(token.value.substring(4));

                            argsBuffer[argi] = new OpContext.Argument(type, regValue);
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
        //imod ireg, ireg, ireg
        String assemblerInput = "imov ireg0 123\nimov ireg1 123\nimul ireg2 ireg1 ireg0";
        List<Assembler.Lexer.Token> asmTokens = Assembler.Lexer.lex(assemblerInput);
        System.out.println(assemblerInput);
//        System.out.println(asmTokens);

        List<Assembler.CompleteInstruction> instructions = Assembler.Parser.parse(asmTokens);

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try {
            BinaryRepresentation.writeToOutputStream(instructions, byteStream);

            byte[] rawBytes = byteStream.toByteArray();
            byte[] rawBytesReversed = new byte[rawBytes.length];

            for (int i = 0; i < rawBytes.length; i++) {
                final int j = rawBytesReversed.length-i-1;
                rawBytesReversed[j] = rawBytes[i];
            }

            System.out.println(Arrays.toString(rawBytes));
//            System.out.println(Arrays.toString(rawBytesReversed));
            System.out.printf("array size : %d\n", byteStream.toByteArray().length);


//            System.out.println(instructions);
            Assembler.CompleteInstruction[] moreInstructions =
                    BinaryRepresentation.fromInputStream(new ByteArrayInputStream(rawBytes))
                            .instructionBlock.completeInstructions;

            System.out.println(Arrays.toString(moreInstructions));

            RegisterMachine registerMachine = new RegisterMachine();
            registerMachine.loadProgramAndResetIp(List.of(moreInstructions));
            System.out.println("ireg: " + Arrays.toString(registerMachine.state.ireg));
            registerMachine.stepUntilHalt();
            System.out.println("ireg: " + Arrays.toString(registerMachine.state.ireg));
        } catch (IOException e) {
            e.printStackTrace();
        }

//        String specLangInput = "imov ireg, ireg, ireg";
//        System.out.println(specLangInput);
//
//        List<SpecLanguage.Lexer.Token> specTokens = SpecLanguage.Lexer.lex(specLangInput);
//        System.out.println(specTokens);
//
//        SpecLanguage.Parser.ParseData parseData = SpecLanguage.Parser.parse(specTokens);
//        System.out.println(parseData);
    }

}
