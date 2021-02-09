package miniJava.SyntacticAnalyzer;
import miniJava.ErrorReporter;
public class Parser {

    private Scanner scanner;
    private ErrorReporter reporter;
    private Token token;
    private boolean trace = true;

    public Parser(Scanner scanner, ErrorReporter reporter) {
        this.scanner = scanner;
        this.reporter = reporter;
    }


    /* SyntaxError - unwind parse stack when parse fails*/
    class SyntaxError extends Error {
        private static final long serialVersionUID = 1L;
    }

    public void parse() {
        token = scanner.scan();
        try {
            parseProgram();
        } catch (SyntaxError e) {
        }
    }

    private void parseProgram() throws SyntaxError {
        while (token.kind == TokenKind.CLASS) {
            parseClassDeclaration();
        }
        accept(TokenKind.EOT);
        System.exit(1);
        return;
    }

    private void parseClassDeclaration() {
        accept(TokenKind.CLASS);
        accept(TokenKind.ID);
        accept(TokenKind.LCURLY);
        while (token.kind != TokenKind.RCURLY) {
            parseFieldMethodDeclaration();
        }
        accept(TokenKind.RCURLY);
        return;
    }

    private void parseFieldMethodDeclaration() {
        boolean methodDeclaration = false;
        parseVisibility();
        parseAccess();
        if (token.kind == TokenKind.VOID) {
            acceptIt();
            methodDeclaration = true;
        } else {
            parseType();
        }
        accept(TokenKind.ID);
        if (token.kind == TokenKind.SEMICOL) //We now know it is a field declaration
            acceptIt();
        else { //Otherwise it has to be a method declaration
            accept(TokenKind.LPAREN);
            if (token.kind != TokenKind.RPAREN) {
                parseParameterList();
            }
            accept(TokenKind.RPAREN); //Parameter list complete
            accept(TokenKind.LCURLY); //Begin statement
            while(token.kind != TokenKind.RCURLY){
                parseStatement();
            }
            accept(TokenKind.RCURLY);
        }
        return;
    }

    private void parseStatement() {
        if (token.kind == TokenKind.LCURLY) { //LCURLY for block begin
            acceptIt();
            while (token.kind != TokenKind.RCURLY) {
                parseStatement();
            }
            accept(TokenKind.RCURLY);
            return;
        } else if (token.kind == TokenKind.RETURN) {
            acceptIt();
            if (token.kind != TokenKind.SEMICOL) {
                parseExpression();
            }
            accept(TokenKind.SEMICOL);
            return;
        } else if (token.kind == TokenKind.IF || token.kind == TokenKind.ELSE) {
            if (token.kind == TokenKind.IF) {
                acceptIt();
                accept(TokenKind.LPAREN);
                parseExpression();
                accept(TokenKind.RPAREN);
                parseStatement();
            } else {
                acceptIt();
                accept(TokenKind.LPAREN);
                parseStatement();
                accept(TokenKind.RPAREN);
            }
            if (token.kind == TokenKind.SEMICOL) {
                acceptIt();
            }
            if (token.kind == TokenKind.ELSE) {
                acceptIt();
                parseStatement();
            } else if (token.kind == TokenKind.RPAREN) {
                acceptIt();
            }
            return;
        } else if (token.kind == TokenKind.WHILE) {
            acceptIt();
            accept(TokenKind.LPAREN);
            parseExpression();
            accept(TokenKind.RPAREN);
            parseStatement();
            return;
        }
        if (token.kind == TokenKind.THIS) {
            //We know it is reference
            parseReference();
            if (token.kind == TokenKind.EQUALS) {
                acceptIt();
                parseExpression();
                accept(TokenKind.SEMICOL);
                return;
            } else if (token.kind == TokenKind.LSQUARE) {
                acceptIt();
                parseExpression();
                accept(TokenKind.RSQUARE);
                accept(TokenKind.EQUALS);
                parseExpression();
                accept(TokenKind.SEMICOL);
                return;
            } else if (token.kind == TokenKind.LPAREN) {
                acceptIt();
                boolean check = checkIfArgumentList();
                if (check) {
                    parseArgumentList();
                }
                accept(TokenKind.RPAREN);
                accept(TokenKind.SEMICOL);
                return;

            }
        } else if (token.kind == TokenKind.INT || token.kind == TokenKind.BOOLEAN) {
            //We know it is type
            parseType();
            if (token.kind == TokenKind.EQUALS) {
                acceptIt();
            }
            parseExpression();
            accept(TokenKind.SEMICOL);
            return;

        } else if (token.kind == TokenKind.ID){
            acceptIt();
            while(token.kind == TokenKind.PERIOD){
                acceptIt();
                accept(TokenKind.ID);
            }
            //If it is already EQUALS, LSQUARE or LPAREN then it must be Reference
            if (token.kind == TokenKind.EQUALS || token.kind == TokenKind.LSQUARE || token.kind == TokenKind.LPAREN){
                if (token.kind == TokenKind.EQUALS){
                    acceptIt();
                    while (token.kind != TokenKind.SEMICOL) {
                        parseExpression();
                    }
                    accept(TokenKind.SEMICOL);
                }
                else if (token.kind == TokenKind.LSQUARE){
                    acceptIt();
                    parseExpression();
                    accept(TokenKind.RSQUARE);
                    accept(TokenKind.EQUALS);
                    parseExpression();
                    accept(TokenKind.SEMICOL);
                }
                else if (token.kind == TokenKind.LPAREN){
                    acceptIt();
                    if(token.kind != TokenKind.RPAREN){
                        parseArgumentList();
                    }
                    accept(TokenKind.RPAREN);
                    accept(TokenKind.SEMICOL);
                }
            }
            //Otherwise it is type
            else{
                parseType();
                accept(TokenKind.ID);
                accept(TokenKind.EQUALS);
                parseExpression();
                accept(TokenKind.SEMICOL);
            }
        }
    }

    private boolean checkIfArgumentList() {
        TokenKind kind = token.kind;
        return kind == TokenKind.THIS || kind == TokenKind.ID || kind == TokenKind.EXCLAMATION ||
                kind == TokenKind.MINUS || kind == TokenKind.LPAREN || kind == TokenKind.NUM ||
                kind == TokenKind.TRUE || kind == TokenKind.FALSE || kind == TokenKind.NEW;
    }

    private void parseExpression() {
        //First possibility - Reference
        /*
Expression ::=
 Reference
 | Reference [ Expression ]
| Reference ( ArgumentList? )
| unop Expression
| Expression binop Expression
| ( Expression )
| num | true | false
| new ( id () | int [ Expression ] | id [ Expression ] )
         */
        if (token.kind.toSimple() == SimpleToken.UNOP || token.kind == TokenKind.MINUS ||
                token.kind == TokenKind.EXCLAMATION || token.kind == TokenKind.EQUALS) { //unop section
            acceptIt();
            parseExpression();
            return;
        } else if (token.kind == TokenKind.LPAREN) { //Parentheses Expression section
            acceptIt();
            parseExpression();
            if (token.kind == TokenKind.PERIOD) {
                parseExpression();
            }
            accept(TokenKind.RPAREN);
            return;
        } else if (token.kind == TokenKind.NUM || token.kind == TokenKind.TRUE ||
                token.kind == TokenKind.FALSE) { //num/true/false section
            acceptIt();
            return;
        } else if (token.kind == TokenKind.NEW) { //new section
            acceptIt();
            if (token.kind == TokenKind.ID) { //Accept id() for new
                acceptIt();
                if (token.kind == TokenKind.LPAREN) {
                    acceptIt();
                    accept(TokenKind.RPAREN);
                } else { //Accept id [ Expression ] for new
                    accept(TokenKind.LSQUARE);
                    parseExpression();
                    accept(TokenKind.RSQUARE);
                    return;
                }
            } else { // Accept int [ Expression ] for new
                accept(TokenKind.INT);
                accept(TokenKind.LSQUARE);
                parseExpression();
                accept(TokenKind.RSQUARE);
                return;
            }
        } else if (token.kind != TokenKind.NUM || token.kind != TokenKind.TRUE || token.kind != TokenKind.FALSE) {
            if (token.kind.toSimple() == SimpleToken.BINOP) {
                acceptIt();
                if (token.kind.toSimple() == SimpleToken.BINOP) {
                    acceptIt();
                }
                parseExpression();
                return;
            }
            parseReference(); //Only reference section
            if (token.kind == TokenKind.LSQUARE) { //Reference into Square Expression section
                accept(TokenKind.LSQUARE);
                parseExpression();
                accept(TokenKind.RSQUARE);
            } else if (token.kind == TokenKind.LPAREN) { //Argument list section
                accept(TokenKind.LPAREN);
                if (token.kind != TokenKind.RPAREN) {
                    parseArgumentList();
                }
                accept(TokenKind.RPAREN);
            }
            if (token.kind.toSimple() == SimpleToken.BINOP) {
                if (token.kind.toSimple() == SimpleToken.BINOP)
                    acceptIt();
                if (token.kind.toSimple() != SimpleToken.BINOP)
                    parseExpression();
                else {
                    acceptIt();
                    parseExpression();
                }
                return;
            }
        }
    }

    //Reference -> id Reference' | this Reference'
    private void parseReference() {
        if (token.kind == TokenKind.THIS || token.kind == TokenKind.ID) {
            acceptIt();
        }
        while (token.kind == TokenKind.PERIOD) {
            acceptIt();
            accept(TokenKind.ID);
        }
        return;
    }

    private void parseArgumentList() {
        parseExpression();
        if (token.kind == TokenKind.COMMA || token.kind == TokenKind.PERIOD) {
            acceptIt();
            parseArgumentList();
        }
//        if (token.kind == TokenKind.RPAREN){
//            acceptIt();
//        }
    }

    private void parseParameterList() {
        parseType();
        accept(TokenKind.ID);
        if (token.kind == TokenKind.COMMA) {
            acceptIt();
            parseParameterList();
        }
    }

    private void parseType() {
        if (token.kind == TokenKind.BOOLEAN) {
            acceptIt();
            return;
        }
        if (token.kind == TokenKind.ID || token.kind == TokenKind.INT) {
            acceptIt();
            if (token.kind == TokenKind.LSQUARE) {
                acceptIt();
                accept(TokenKind.RSQUARE);
            }
        }
    }

    private void parseAccess() {
        if (token.kind == TokenKind.STATIC) {
            acceptIt();
            return;
        }
        return;
    }

    private void parseVisibility() {
        if (token.kind == TokenKind.PUBLIC) {
            acceptIt();
            return;
        } else if (token.kind == TokenKind.PRIVATE) {
            acceptIt();
            return;
        }
        return;
    }

    private void acceptIt() throws SyntaxError {
        accept(token.kind);
    }

    /* Verify that the current token in inp0ut matches expected token and advance to next token
     */
    private void accept(TokenKind expectedTokenKind) throws SyntaxError {
        if (token.kind == expectedTokenKind || (token.kind.toSimple() == expectedTokenKind.toSimple() &&
                token.kind.toSimple() != null)) {
            if (trace)
                pTrace();
            token = scanner.scan();
        } else
            parseError("expecting '" + expectedTokenKind + "' but found '" + token.kind + "'");
    }

    /* report parse error, unwind call stack to start of parse*/
    private void parseError(String e) throws SyntaxError {
        reporter.reportError("Parse error: " + e);
        throw new SyntaxError();
    }

    /* show parse stack whenever terminal is accepted*/
    private void pTrace() {
        StackTraceElement[] stl = Thread.currentThread().getStackTrace();
        for (int i = stl.length - 1; i > 0; i--) {
            if (stl[i].toString().contains("parse"))
                System.out.println(stl[i]);
        }
        System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
        System.out.println();
    }
}
