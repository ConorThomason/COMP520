package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

public class Scanner {
    private InputStream inputStream;
    private ErrorReporter reporter;
    private char currentChar;
    private StringBuilder currentSpelling;

    private boolean eot = false;

    public Scanner(InputStream inputStream, ErrorReporter reporter) {
        this.inputStream = inputStream;
        this.reporter = reporter;
        readChar();
    }

    public Token scan() {
        while (!eot && (currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r')) {
//            System.out.println("Skipping whitespace");
            skipIt();
        }
        //token starts, get spelling and identify token kind
        currentSpelling = new StringBuilder();
        TokenKind kind = scanToken();
        while (kind == TokenKind.COMMENT || kind == TokenKind.BLOCKCOMMENT) {
            while (currentChar != '\n' && currentChar != '\r' && !eot) {
                skipIt();
            }
            skipIt();
            kind = scanToken();
            if (kind != TokenKind.COMMENT && kind != TokenKind.BLOCKCOMMENT)
                break;
            if (kind == TokenKind.COMMENT)
                continue;
            boolean foundEnd = false;
            while(!foundEnd) {
                boolean foundStar = false;
                while (!foundStar){
                    if (currentChar == '*'){
                        foundStar = true;
                    }
                    skipIt();
                    if (currentChar == '/'){
                        foundEnd = true;
                    }
                }
            }
        }
        String spelling = currentSpelling.toString();
        System.out.println(spelling);
        System.out.println(kind);
        return new Token(kind, spelling);
    }

    public boolean isNumeric(char c){
        switch(c){
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return true;
            default:
                return false;
        }
    }
    /*Determine token kind*/
    public TokenKind scanToken() {
        if (eot) {
            return TokenKind.EOT;
        }
        boolean idFound = false;
        while (isAlphabetic(currentChar) || currentChar == '_') {
            if (isAlphabetic(currentChar) || currentChar == '_'){
                takeIt();
                idFound = true;
            }
            if (idFound && isNumeric(currentChar)){
                while (isAlphabetic(currentChar) || currentChar == '_' || isNumeric(currentChar)){
                    takeIt();
                }
            }
        }
        if (idFound) {
            switch(currentSpelling.toString()){
                case "class":
                    return TokenKind.CLASS;
                case "public":
                    return TokenKind.PUBLIC;
                case "private":
                    return TokenKind.PRIVATE;
                case "static":
                    return TokenKind.STATIC;
                case "void":
                    return TokenKind.VOID;
                case "int":
                    return TokenKind.INT;
                case "boolean":
                    return TokenKind.BOOLEAN;
                case "this":
                    return TokenKind.THIS;
                case "return":
                    return TokenKind.RETURN;
                case "if":
                    return TokenKind.IF;
                case "else":
                    return TokenKind.ELSE;
                case "while":
                    return TokenKind.WHILE;
                case "true":
                    return TokenKind.TRUE;
                case "false":
                    return TokenKind.FALSE;
                case "new":
                    return TokenKind.NEW;
            }
            return TokenKind.ID;
        }
        char prevChar;

        //Scan token
        switch (currentChar) {
            case '+':
                takeIt();
                return (TokenKind.PLUS);
            case '*':
                takeIt();
                if (currentChar == '/'){
                    return (TokenKind.BLOCKCOMMENTEND);
                }
                return (TokenKind.TIMES);
            case '/':
                takeIt();
                if (currentChar == '/'){
                    skipIt();
                    currentSpelling.deleteCharAt(currentSpelling.length() - 1);
                    return (TokenKind.COMMENT);
                }
                else if (currentChar == '*'){
                    skipIt();
                    currentSpelling.deleteCharAt(currentSpelling.length() - 1);
                    return (TokenKind.BLOCKCOMMENT);
                }
                else{
                    takeIt();
                }
                return (TokenKind.DIVIDE);
            case '=':
                takeIt();
                return (TokenKind.EQUALS);
            case '<':
                takeIt();
                return (TokenKind.LTHAN);
            case '>':
                takeIt();
                return (TokenKind.GTHAN);
            case '&':
                takeIt();
                return (TokenKind.ANDSYMBOL);
            case '|':
                takeIt();
                return (TokenKind.ORSYMBOL);
            case '!':
                takeIt();
                return (TokenKind.EXCLAMATION);
            case '-':
                takeIt();
                return (TokenKind.MINUS);
            case '[':
                takeIt();
                return (TokenKind.LSQUARE);
            case ']':
                takeIt();
                return (TokenKind.RSQUARE);
            case '{':
                takeIt();
                return (TokenKind.LCURLY);
            case '}':
                takeIt();
                return (TokenKind.RCURLY);
            case '(':
                takeIt();
                return (TokenKind.LPAREN);
            case ')':
                takeIt();
                return (TokenKind.RPAREN);
            case '.':
                takeIt();
                return (TokenKind.PERIOD);
            case ';':
                takeIt();
                return (TokenKind.SEMICOL);
            case '\t':
            case '\r':
            case '\n':
            case ' ':
                skipIt();
                return scanToken();
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                boolean leadingZero = true;
                while (isDigit(currentChar)) {
                    if (leadingZero && currentChar == '0') {
                        skipIt();
                        continue;
                    } else {
                        leadingZero = false;
                        takeIt();
                    }
                }
                return (TokenKind.NUM);
        }
        scanError("Unrecognized character '" + currentChar + "' in input");
        return (TokenKind.ERROR);
    }

    private boolean isAlphabetic(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private void takeIt() {
        currentSpelling.append(currentChar);
        nextChar();
    }

    private void skipIt() {
        nextChar();
    }

    private boolean isDigit(char c) {
        return (c >= '0') && (c <= '9');
    }

    private void scanError(String m) {
        reporter.reportError("Scan Error: " + m);
    }

    private final static char eolUnix = '\n';
    private final static char eolWindows = '\r';

    private void nextChar() {
        if (!eot)
            readChar();
    }

    private void readChar() {
        try {
            int c = inputStream.read();
            currentChar = (char) c;
            if (c == -1)
                eot = true;
        } catch (IOException e) {
            scanError("I/O Exception");
            eot = true;
        }
    }
}
