package miniJava.SyntacticAnalyzer;

public class Parser {

    private Scanner scanner;
    private ErrorReporter reporter;
    private Token token;
    private boolean trace = true;

    public Parser(Scanner scanner, ErrorReporter reporter){
        this.scanner = scanner;
        this.reporter = reporter;
    }


    /* SyntaxError - unwind parse stack when parse fails*/
    class SyntaxError extends Error{
        private static final long serialVersionUID = 1L;
    }

    public void parse() {
        token = scanner.scan();
        try{
            parseProgram();
        }
        catch (SyntaxError e){};
    }

    private void parseProgram() throws SyntaxError {
        accept(TokenKind.CLASS);
        accept(TokenKind.ID);
    }

    private void acceptIt() throws SyntaxError{
        accept(token.kind);
    }

    /* Verify that the current token in inp0ut matches expected token and advance to next token
     */
    private void accept(TokenKind expectedTokenKind) throws SyntaxError{
        if (token.kind == expectedTokenKind){
            if (trace)
                pTrace();
            token = scanner.scan();
        }
        else
            parseError("expecting '" + expectedTokenKind + "' but found '" + token.kind + "'");
    }

    /* report parse error, unwind call stack to start of parse*/
    private void parseError(String e) throws SyntaxError{
        reporter.reportError("Parse error: " + e);
        throw new SyntaxError();
    }

    /* show parse stack whenever terminal is accepted*/
    private void pTrace(){
        StackTraceElement [] stl = Thread.currentThread().getStackTrace();
        for (int i = stl.length - 1; i > 0 ; i--){
            if(stl[i].toString().contains("parse"))
                System.out.println(stl[i]);
        }
        System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
        System.out.println();
    }
}
