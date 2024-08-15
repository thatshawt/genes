package xyz.davidpineiro.genes.core.evolution.asm.registerMachine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * its a C-like language
 * WARNING: this is almost useless right now
 */
public class RegisterClang {

    public static final class Lexer{
        enum TokenType{
            KEYWORD, IDENTIFIER,
            LPAREN, RPAREN, RCURLBRACKET, LCURLBRACKET, LBRACKET, RBRACKET,
            LARROW, RARROW, DOT,
            SEMICOLON, COLON, COMMA, EQUALS,
            AND_BITWISE, XOR, LSHIFT, RSHIFT, NOT, OR_BITWISE,
            PLUS, MINUS, STAR, FSLASH, BSLASH,

            PLUS_EQUALS, MINUS_EQUALS, TIMES_EQUALS, DIV_EQUALS, EQUAL_EQUAL, XOR_EQUALS,AND_EQUALS,
            GT_EQUAL, LT_EQUAL, POINTER_ARROW, NOT_EQUAL, OR, AND,
            LINE_COMMENT,

            STRING_LITERAL, CHAR_LITERAL, FLOAT_LITERAL, INT_LITERAL, /*BOOL_LITERAL*/
        }
        enum Keyword{
            AUTO, /*DOUBLE,*/ INT, STRUCT,
            BREAK, ELSE, LONG, SWITCH,
            CASE, ENUM, /*REGISTER,*/ TYPEDEF,
            CHAR, EXTERN, RETURN, UNION,
            CONST, FLOAT, /*SHORT, UNSIGNED,*/
            CONTINUE, FOR, /*SIGNED,*/ VOID,
            DEFAULT, GOTO, SIZEOF, /*VOLATILE,*/
            DO, IF, STATIC, WHILE;

            public static final String[] ALL_KEYWORDS;

            static{
                String[] strings = new String[Keyword.values().length];

                final Keyword[] keywords =  Keyword.values();
                for(int i = 0;i< keywords.length;i++) {
                    final Keyword keyword = keywords[i];

                    strings[i] = keyword.name().toLowerCase();
                }

                ALL_KEYWORDS = strings;
            }
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
                List.of("-+=,;:*/<>&!^.|".split("")),//length 1 tokens
                List.of("+=,-=,*=,/=,==,^=,&=,<=,>=,<<,>>,->,!=,||,&&,//".split(",")) //length 2 token
        );

        final static List<List<TokenType>> SPECIAL_TOKEN_MAP = List.of(
                List.of(),//length 0 tokens
                List.of(TokenType.MINUS,
                        TokenType.PLUS,TokenType.EQUALS,TokenType.COMMA,
                        TokenType.SEMICOLON, TokenType.COLON, TokenType.STAR,
                        TokenType.FSLASH,
                        TokenType.LARROW,TokenType.RARROW,
                        TokenType.AND_BITWISE,TokenType.NOT,
                        TokenType.XOR, TokenType.DOT, TokenType.OR_BITWISE),//length 1 tokens
                List.of(TokenType.PLUS_EQUALS,TokenType.MINUS_EQUALS,
                        TokenType.TIMES_EQUALS,TokenType.DIV_EQUALS,
                        TokenType.EQUAL_EQUAL,TokenType.XOR_EQUALS, TokenType.AND_EQUALS,
                        TokenType.LT_EQUAL, TokenType.GT_EQUAL,
                        TokenType.LSHIFT,TokenType.RSHIFT,TokenType.POINTER_ARROW,
                        TokenType.NOT_EQUAL,TokenType.OR, TokenType.AND,
                        TokenType.LINE_COMMENT
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

        private static boolean isHexadecimal(char c){
            return "0123456789abcdef".contains(String.valueOf(c).toLowerCase());
        }

        private static boolean isParenThing(char c){
            return "()[]{}".contains(String.valueOf(c));
        }

        enum State{
            WAITING, KEYWORD, BUILD_SPECIAL_TOKEN, PAREN,
            MINUS, INTEGER, HEXADECIMAL, BINARY, INTEGER_DOT, FLOAT,
            STRING, ESCAPE_STRING_CHAR, CHAR, ESCAPE_CHAR,
            LINE_COMMENT,

        }

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
                        }else if(c == '"'){
                            state = State.STRING;
                        }
//                        else if(c == '\''){
//                            state = State.CHAR;
//                        }
                        break;
//                    case CHAR:
//                        if(c == '\''){
//                            tokens.add(new Token(TokenType.CHAR_LITERAL,stringBuffer.toString().charAt(0)));
//                            stringBuffer = new StringBuilder();
//
//                            state = State.WAITING;
//                        }else{
//
//                        }
//                        break;
//                    case ESCAPE_CHAR:
//                        break;
                    case STRING:
                        if(c == '"'){
                            tokens.add(new Token(TokenType.STRING_LITERAL,stringBuffer.toString()));
                            stringBuffer = new StringBuilder();

                            state = State.WAITING;
                        }else if(c == '\\'){
                            state = State.ESCAPE_STRING_CHAR;
                        }else{
                            stringBuffer.append(c);
                        }
                        break;
                    case ESCAPE_STRING_CHAR:
                        switch(c){
                            case '\\':
                                stringBuffer.append('\\');break;
                            case 'n':
                                stringBuffer.append('\n');break;
                            case '"':
                                stringBuffer.append('"');break;
                            case '\'':
                                stringBuffer.append('\'');break;
                            case '?':
                                stringBuffer.append('?');break;
                        }
                        state = State.STRING;
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
                    case INTEGER: {
                        final String bufferRn = stringBuffer.toString();
                        if (Character.isDigit(c)) {
                            stringBuffer.append(c);
                        } else if (c == '.') {
                            state = State.INTEGER_DOT;
                            stringBuffer.append(c);
                        } else if (Character.isWhitespace(c)) {
                            tokens.add(new Token(TokenType.INT_LITERAL, Integer.valueOf(bufferRn)));
                            stringBuffer = new StringBuilder();
                            state = State.WAITING;
                        } else if (isSpecialTokenFirstChar(c)) {
                            tokens.add(new Token(TokenType.INT_LITERAL, Integer.valueOf(bufferRn)));
                            state = State.BUILD_SPECIAL_TOKEN;
                            stringBuffer = new StringBuilder();
                            stringBuffer.append(c);
                        } else if (isParenThing(c)) {
                            tokens.add(new Token(TokenType.INT_LITERAL, Integer.valueOf(bufferRn)));
                            stringBuffer = new StringBuilder();
                            state = State.PAREN;
                            i--;
                        } else if ((bufferRn.equals("-0") || bufferRn.equals("0")) && (c == 'x' || c == 'b')) {
                            if (bufferRn.charAt(0) == '-') {//is negative
                                stringBuffer = new StringBuilder("-");
                            } else {//is positive
                                stringBuffer = new StringBuilder();
                            }

                            if(c == 'b'){
                                state = State.BINARY;
                            }else if(c == 'x'){
                                state = State.HEXADECIMAL;
                            }
                        }
                        break;
                    }
                    case HEXADECIMAL:
                        if(isHexadecimal(c)){
                            stringBuffer.append(c);
                        }else{
                            final int feelingHexy = Integer.parseInt(stringBuffer.toString().toUpperCase(),16);

                            tokens.add(new Token(TokenType.INT_LITERAL, feelingHexy));

                            stringBuffer = new StringBuilder();
                            state = State.WAITING;
                            i--;//let the waiting state deal with it
                        }
                        break;
                    case BINARY:
                        if(c == '0' || c == '1'){
                            stringBuffer.append(c);
                        }else{
                            final int feelingBiny = Integer.parseInt(stringBuffer.toString().toUpperCase(),2);

                            tokens.add(new Token(TokenType.INT_LITERAL, feelingBiny));

                            stringBuffer = new StringBuilder();
                            state = State.WAITING;
                            i--;//let the waiting state deal with it
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
                    case KEYWORD: {
                        final TokenType tokentype;
                        if(List.of(Keyword.ALL_KEYWORDS).contains(stringBuffer.toString())){
                            tokentype = TokenType.KEYWORD;
                        }else{
                            tokentype = TokenType.IDENTIFIER;
                        }

                        if (Character.isWhitespace(c)) {
                            tokens.add(new Token(tokentype, stringBuffer.toString()));
                            stringBuffer = new StringBuilder();
                            state = State.WAITING;
                        } else if (Character.isLetterOrDigit(c) || c == '_') {
                            stringBuffer.append(c);
                        } else if (isSpecialTokenFirstChar(c)) {
                            tokens.add(new Token(tokentype, stringBuffer.toString()));
                            stringBuffer = new StringBuilder();
                            state = State.BUILD_SPECIAL_TOKEN;
                            i--;//so we can recognize in WAITING state
                        } else if (isParenThing(c)) {
                            tokens.add(new Token(tokentype, stringBuffer.toString()));
                            stringBuffer = new StringBuilder();
                            state = State.PAREN;
                            i--;
                        }
                        break;
                    }
                    case BUILD_SPECIAL_TOKEN:
                        if(c == '*'){
                            tokens.add(new Token(TokenType.STAR, null));
                            stringBuffer = new StringBuilder();
                            state = State.WAITING;
                        }else if(Character.isWhitespace(c) || Character.isLetterOrDigit(c) || isParenThing(c)){
                            final String tokenString = stringBuffer.toString();
                            final int index = SPECIAL_TOKENS.get(tokenString.length()).indexOf(tokenString);
                            final TokenType tokenType = SPECIAL_TOKEN_MAP.get(tokenString.length()).get(index);

                            if(tokenType == TokenType.LINE_COMMENT){
                                state = State.LINE_COMMENT;
                                stringBuffer = new StringBuilder();
                                continue;
                            }

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
                    case LINE_COMMENT:
                        if(c == '\n'){
                            state = State.WAITING;
                        }
                        break;
                }
            }

            return tokens.toArray(new Token[0]);
        }

    }

/*
void obs_extract_hevc_headers(const uint8_t *packet, size_t size,
			      uint8_t **new_packet_data,
			      size_t *new_packet_size, uint8_t **header_data,
			      size_t *header_size, uint8_t **sei_data,
			      size_t *sei_size)
{
	DARRAY(uint8_t) new_packet;
	DARRAY(uint8_t) header;
	DARRAY(uint8_t) sei;
	const uint8_t *nal_start, *nal_end, *nal_codestart;
	const uint8_t *end = packet + size;

	da_init(new_packet);
	da_init(header);
	da_init(sei);

	nal_start = obs_nal_find_startcode(packet, end);
	nal_end = NULL;
	while (nal_end != end) {
		nal_codestart = nal_start;

		while (nal_start < end && !*(nal_start++))
			;

		if (nal_start == end)
			break;

		const uint8_t type = (nal_start[0] & 0x7F) >> 1;

		nal_end = obs_nal_find_startcode(nal_start, end);
		if (!nal_end)
			nal_end = end;

		if (type == OBS_HEVC_NAL_VPS || type == OBS_HEVC_NAL_SPS ||
		    type == OBS_HEVC_NAL_PPS) {
			da_push_back_array(header, nal_codestart,
					   nal_end - nal_codestart);
		} else if (type == OBS_HEVC_NAL_SEI_PREFIX ||
			   type == OBS_HEVC_NAL_SEI_SUFFIX) {
			da_push_back_array(sei, nal_codestart,
					   nal_end - nal_codestart);

		} else {
			da_push_back_array(new_packet, nal_codestart,
					   nal_end - nal_codestart);
		}

		nal_start = nal_end;
	}

	*new_packet_data = new_packet.array;
	*new_packet_size = new_packet.num;
	*header_data = header.array;
	*header_size = header.num;
	*sei_data = sei.array;
	*sei_size = sei.num;
}
*/

    public static final class Parser{
        /*
         * functionDefinition: functionPrototype codeBlock
         *
         * functionDeclaration: functionPrototype Semicolon
         *
         * functionPrototype:
         * | Keyword? type Identifier argList
         *
         * ifStatement:
         * | Keyword(if) LParen boolExpression RParen codeBlock
         *   (Keyword(else if) LParen boolExpression RParen codeBlock)+
         *   (Keyword(else) LParen boolExpression RParen codeBlock)?
         *
         * whileStatement:
         * | Keyword(while) LParen boolExpression RParen codeBlock
         *
         * //for(int i=0;i<10;i++){}
         * forLoopStatement:
         * | Keyword(for) LParen expression Semicolon
         *                       boolExpression Semicolon
         *                       expression
         *                RParen
         *                codeBlock
         *
         * argList:
         * | LParen (type Identifier Comma(args - 1 commas))* RParen
         *
         *
         *
         * boolExpression:
         * | True
         * | False
         * | LParen boolExpression RParen
         * | boolExpression And boolExpression
         * | boolExpression Or boolExpression
         * | Exclamation boolExpression
         *
         *
         * // int bruh = 5;
         * assignmentStatement:
         * | type Identifier assignmentOps expression Semicolon
         *
         *
         * // 5 + 5, 1, hello+1
         * expression:
         * |
         *
         * assignmentOps:
         * | Equal
         * | PlusEqual
         * | MinusEqual
         * | TimesEqual
         * | DivEquals
         *
         * type: Identifier Star?
         *
         * codeBlock:
         *
         *
         */
    }

    public static void main(String[] args) {
        final String input = "static const char adjvs[][10] = {\n" +
                "    \"SWINGING\", \"RAD\\n\\\\\\\'\\\"IANT\", \"BRONZE\", \"RED\", \"GREEN\", \"PINK\", \"STONE\",\n" +
                "    \"PURPLE\", \"NAVY\", \"SCARLET\", \"FALLEN\", \"FUCHSIA\", \"EMERALD\", \"DESERT\",\n" +
                "    \"SACRED\", \"FROZEN\", \"CELTIC\", \"SWOOPING\", \"LEAD\", \"TURQUOISE\", \"GRAY\",\n" +
                "    \"BEIGE\", \"OLIVE\", \"SILENT\", \"MAROON\", \"GLASS\", \"FADING\", \"CRIMSON\",\n" +
                "    \"BLUE\", \"AMBER\", \"TEAL\", \"BLACK\", \"FLYING\", \"COPPER\", \"LIME\", \"GOLDEN\",\n" +
                "    \"VENGEFUL\", \"AGING\", \"EMPTY\", \"STEEL\", \"IRON\", \"SPECTRAL\", \"LOST\",\n" +
                "    \"WHITE\", \"FROWNING\", \"YELLOW\", \"FORGOTTEN\", \"AQUA\", \"PLASTIC\", \"LAZY\",\n" +
                "    \"SILVER\", \"SWIFT\", \"CHASING\",\n" +
                "};";
        Lexer.Token[] tokens = Lexer.lex(input);
        System.out.println(input);
        System.out.println(Arrays.toString(tokens));
    }

}
