package xyz.davidpineiro.genes.core.evolution.asm.registerMachine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**

 int hello;

 int hello = 30;

 void main();

 void main(){}

 void main(char* args);

 void main(char* args){}

 void main(char* args){
    int hello;
    int hello = 49;
    printf();
    printf("sussy");
 }

 */
public class RegisterClang {

    public static class Lexer{
        enum TokenType{
            KEYWORD,
            LPAREN, RPAREN, RCURLBRACKET, LCURLBRACKET, LBRACKET, RBRACKET,
            LARROW, RARROW,
            SEMICOLON, COMMA, EQUALS,
            AMPERSTAND, CAROT, EXCLAMATION,
            PLUS, MINUS, STAR, FSLASH, BSLASH,

            PLUS_EQUALS, MINUS_EQUALS, TIMES_EQUALS, DIV_EQUALS, EQUAL_EQUAL,
            GT_EQUAL, LT_EQUAL, POINTER_ARROW, NOT_EQUAL, OR, AND,

            STRING_LITERAL, CHAR_LITERAL, FLOAT_LITERAL, INT_LITERAL, BOOL_LITERAL
        }
        static class Token{
            public final TokenType tokenType;
            public final Object value;

            public Token(TokenType tokenType, Object value) {
                this.tokenType = tokenType;
                this.value = value;
            }

            @Override
            public String toString() {
                return String.format("(%s,%s)", tokenType.name(), value);
            }
        }

        final static List<List<String>> SPECIAL_TOKENS = List.of(
                List.of(),//length 0 tokens
                List.of("-+=,;*/<>&!^".split("")),//length 1 tokens
                List.of("+=,-=,*=,/=,==,<=,>=,->,!=,||,&&".split(",")) //length 2 token
        );

        final static List<List<TokenType>> SPECIAL_TOKEN_MAP = List.of(
                List.of(),//length 0 tokens
                List.of(TokenType.MINUS,
                        TokenType.PLUS,TokenType.EQUALS,TokenType.COMMA,
                        TokenType.SEMICOLON,TokenType.STAR,TokenType.FSLASH,
                        TokenType.LARROW,TokenType.RARROW,
                        TokenType.AMPERSTAND,TokenType.EXCLAMATION,
                        TokenType.CAROT),//length 1 tokens
                List.of(TokenType.PLUS_EQUALS,TokenType.MINUS_EQUALS,
                        TokenType.TIMES_EQUALS,TokenType.DIV_EQUALS,
                        TokenType.EQUAL_EQUAL,TokenType.LT_EQUAL,
                        TokenType.GT_EQUAL,TokenType.POINTER_ARROW,
                        TokenType.NOT_EQUAL,TokenType.OR, TokenType.AND
                ) //length 2 token
        );

        private static boolean isSpecialTokenFirstChar(char c){
            if(SPECIAL_TOKENS.get(1).contains(String.valueOf(c)))return true;

            for(int i=1;i<SPECIAL_TOKENS.size();i++){
                final List<String> specials = SPECIAL_TOKENS.get(i);
                for(String specialToken : specials){
                    if(specialToken.startsWith(String.valueOf(c)))return true;
                }
            }

            return false;
        }

        private static boolean isSpecialTokenSecondChar(char c){
            for(int i=2;i<SPECIAL_TOKENS.size();i++){
                final List<String> specials = SPECIAL_TOKENS.get(i);
                for(String specialToken : specials){
                    if(specialToken.charAt(1) == c)return true;
                }
            }

            return false;
        }

        private static boolean isParenThing(char c){
            return "()[]{}".contains(String.valueOf(c));
        }

        enum State{WAITING, KEYWORD, BUILD_SPECIAL_TOKEN, PAREN, MINUS, INTEGER, INTEGER_DOT, FLOAT, STRING}

        public static Token[] lex(String input){
            State state = State.WAITING;

            List<Token> tokens = new ArrayList<>();
            StringBuilder stringBuffer = new StringBuilder();

            final String paddedInput = input+" ";

            for(int i = 0;i < paddedInput.length();i++){
                final char c = paddedInput.charAt(i);
                System.out.printf("c: '%s', buffer: '%s', state: %s\n", c, stringBuffer,state.name());
                switch(state){
                    case WAITING:
                        if(Character.isAlphabetic(c)){
                            state = State.KEYWORD;
                            stringBuffer.append(c);
                        }else if(c == '-'){//minus takes priority over other special chars
                            state = State.MINUS;
                            stringBuffer.append(c);
                        }else if(isParenThing(c)){
                            state = State.PAREN;
                            i--;
                        }else if(isSpecialTokenFirstChar(c)){
                            state = State.BUILD_SPECIAL_TOKEN;
                            i--;
                            stringBuffer = new StringBuilder();
                        }else if(Character.isDigit(c)){
                            state = State.INTEGER;
                            stringBuffer.append(c);
                        }
                        break;
                    case PAREN:
                        if(c == '('){
                            tokens.add(new Token(TokenType.LPAREN, null));
                            stringBuffer = new StringBuilder();
                        }else if(c == ')'){
                            tokens.add(new Token(TokenType.RPAREN, null));
                            stringBuffer = new StringBuilder();
                        }else if(c == '['){
                            tokens.add(new Token(TokenType.LBRACKET, null));
                            stringBuffer = new StringBuilder();
                        }else if(c == ']'){
                            tokens.add(new Token(TokenType.RBRACKET, null));
                            stringBuffer = new StringBuilder();
                        }else if(c == '{'){
                            tokens.add(new Token(TokenType.LCURLBRACKET, null));
                            stringBuffer = new StringBuilder();
                        }else if(c == '}'){
                            tokens.add(new Token(TokenType.RCURLBRACKET, null));
                            stringBuffer = new StringBuilder();
                        }
                        state = State.WAITING;
                        break;
                    case MINUS:
                        if(Character.isDigit(c)){
                            state = State.INTEGER;
                            stringBuffer.append(c);
                        }else if(isSpecialTokenSecondChar(c)){
                            state = State.BUILD_SPECIAL_TOKEN;
                            stringBuffer.append(c);
                        }else if(Character.isLetter(c)){
                            tokens.add(new Token(TokenType.MINUS, null));
                            stringBuffer = new StringBuilder();
                            stringBuffer.append(c);
                            state = State.KEYWORD;
                        }else if(Character.isWhitespace(c)){
                            tokens.add(new Token(TokenType.MINUS, null));
                            stringBuffer = new StringBuilder();
                            state = State.WAITING;
                        }
                        break;
                    case INTEGER:
                        if(Character.isDigit(c)){
                            stringBuffer.append(c);
                        }else if(c == '.'){
                            state = State.INTEGER_DOT;
                            stringBuffer.append(c);
                        }else if(Character.isWhitespace(c)){
                            tokens.add(new Token(TokenType.INT_LITERAL, Integer.valueOf(stringBuffer.toString())));
                            stringBuffer = new StringBuilder();
                            state = State.WAITING;
                        }else if(isSpecialTokenFirstChar(c)){
                            tokens.add(new Token(TokenType.INT_LITERAL, Integer.valueOf(stringBuffer.toString())));
                            state = State.BUILD_SPECIAL_TOKEN;
                            stringBuffer = new StringBuilder();
                            stringBuffer.append(c);
                        }else if(isParenThing(c)){
                            tokens.add(new Token(TokenType.INT_LITERAL, Integer.valueOf(stringBuffer.toString())));
                            stringBuffer = new StringBuilder();
                            state = State.PAREN;
                            i--;
                        }
                        break;
                    case INTEGER_DOT:
                        if(Character.isDigit(c)){
                            state = State.FLOAT;
                            stringBuffer.append(c);
                        }
                        break;
                    case FLOAT:
                        if(Character.isDigit(c)){
                            stringBuffer.append(c);
                        }else if(Character.isWhitespace(c)){
                            tokens.add(new Token(TokenType.FLOAT_LITERAL, Float.valueOf(stringBuffer.toString())));
                            state = State.WAITING;
                            stringBuffer = new StringBuilder();
                        }else if(isSpecialTokenFirstChar(c)){
                            tokens.add(new Token(TokenType.FLOAT_LITERAL, Float.valueOf(stringBuffer.toString())));
                            state = State.BUILD_SPECIAL_TOKEN;
                            stringBuffer = new StringBuilder();
                            stringBuffer.append(c);
                        }else if(isParenThing(c)){
                            tokens.add(new Token(TokenType.FLOAT_LITERAL, Float.valueOf(stringBuffer.toString())));
                            stringBuffer = new StringBuilder();
                            state = State.PAREN;
                            i--;
                        }
                        break;
                    case KEYWORD:
                        if(Character.isWhitespace(c)){
                            tokens.add(new Token(TokenType.KEYWORD, stringBuffer.toString()));
                            stringBuffer = new StringBuilder();
                            state = State.WAITING;
                        }else if(Character.isLetterOrDigit(c) || c == '_'){
                            stringBuffer.append(c);
                        }else if(isSpecialTokenFirstChar(c)){
                            tokens.add(new Token(TokenType.KEYWORD, stringBuffer.toString()));
                            stringBuffer = new StringBuilder();
                            state = State.BUILD_SPECIAL_TOKEN;
                            i--;//so we can recognize in WAITING state
                        }else if(isParenThing(c)){
                            tokens.add(new Token(TokenType.KEYWORD, stringBuffer.toString()));
                            stringBuffer = new StringBuilder();
                            state = State.PAREN;
                            i--;
                        }
                        break;
                    case BUILD_SPECIAL_TOKEN:
                        if(Character.isWhitespace(c) || Character.isLetterOrDigit(c) || isParenThing(c)){
                            final String tokenString = stringBuffer.toString();
                            final int index = SPECIAL_TOKENS.get(tokenString.length()).indexOf(tokenString);
                            final TokenType tokenType = SPECIAL_TOKEN_MAP.get(tokenString.length()).get(index);
                            tokens.add(new Token(tokenType, null));
                            if(isParenThing(c)){
                                stringBuffer = new StringBuilder();
                                state = State.PAREN;
                                i--;
                            }else{
                                state = State.WAITING;
                                stringBuffer = new StringBuilder();
                                i--;
                            }

                        }else{//TODO: maybe i need to check if it is a special character instread of assumming it is
                            stringBuffer.append(c);
                        }
                        break;
                }
            }

            return tokens.toArray(new Token[0]);
        }

    }

    public static void main(String[] args) {
        final String input = "- &&sussyBaka_balls = 72; -= -2 void main (char* args){printf();}";
        Lexer.Token[] tokens = Lexer.lex(input);
        System.out.println(input);
        System.out.println(Arrays.toString(tokens));
    }

}
