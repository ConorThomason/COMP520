package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

import com.sun.org.apache.bcel.internal.classfile.SourceFile;
import miniJava.ErrorReporter;
public class Scanner {
    private InputStream inputStream;
    private ErrorReporter reporter;
    private char currentChar;
    private StringBuilder currentSpelling;

    private boolean eot = false;
    int lines = 1;
    public Scanner(InputStream inputStream, ErrorReporter reporter) {
        this.inputStream = inputStream;
        this.reporter = reporter;
        readChar();
    }

    public Token scan() {
        int startLine = lines;
        while (!eot && (currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r')) {
//            System.out.println("Skipping whitespace");
            if (currentChar == '\n')
                lines++;
            skipIt();
        }
        //token starts, get spelling and identify token kind
        currentSpelling = new StringBuilder();
        TokenKind kind = scanToken(true);
        boolean blockCommentTerminated = !(kind == TokenKind.BLOCKCOMMENT);
        while (kind == TokenKind.COMMENT || kind == TokenKind.BLOCKCOMMENT) {
            while (kind == TokenKind.COMMENT) {
                while (currentChar != '\n' && currentChar != '\r' && !eot) {
                    if (currentChar == '\n')
                        lines++;
                    skipIt();
                }
                skipIt();
                if (kind != TokenKind.COMMENT) {
                    kind = scanToken(true);
                    break;
                }
                if (kind == TokenKind.COMMENT) {
                    kind = scanToken(true);
                    continue;
                }
            }
            if (kind == TokenKind.BLOCKCOMMENT) {
                while (kind != TokenKind.BLOCKCOMMENTEND && !eot) {
                    currentSpelling.delete(0, currentSpelling.length());
                    kind = scanToken(true);
                }
                if (eot && kind != TokenKind.BLOCKCOMMENTEND) {
                    scanError("Block comment unterminated");
                } else {
                    currentSpelling.delete(0, currentSpelling.length());
                    kind = scanToken(true);
                }
            }
        }

        String spelling = currentSpelling.toString();
//        System.out.println(spelling);
//        System.out.println(kind);
        return new Token(kind, spelling, new SourcePosition(startLine, lines));
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
    public TokenKind scanToken(boolean comment) {
        if (eot) {
            return TokenKind.EOT;
        }
        boolean idFound = false;
        while (isAlphabetic(currentChar) || currentChar == '_') {
            if (isAlphabetic(currentChar) || currentChar == '_'){
                if (!idFound && currentChar == '_'){
                    return TokenKind.ERROR;
                }
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
                case "null":
                    return TokenKind.NULL;
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
                    takeIt();
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
                    return (TokenKind.DIVIDE);
                }
            case '=':
                takeIt();
                if (currentChar == '='){
                    takeIt();
                    return TokenKind.EQUIVALENT;
                }
                else{
                    return TokenKind.EQUALS;
                }
            case '<':
                takeIt();
                if (currentChar == '='){
                    takeIt();
                    return TokenKind.LEQUAL;
                }
                return (TokenKind.LTHAN);
            case '>':
                takeIt();
                if (currentChar == '='){
                    takeIt();
                    return TokenKind.GEQUAL;
                }
                return (TokenKind.GTHAN);
            case '&':
                takeIt();
                if (currentChar != '&'){
                    return TokenKind.ERROR;
                }
                else {
                    takeIt();
                    return (TokenKind.AND);
                }
            case '|':
                takeIt();
                if (currentChar != '|'){
                    return TokenKind.ERROR;
                } else {
                    takeIt();
                    return (TokenKind.OR);
                }
            case '!':
                takeIt();
                if (currentChar == '='){
                    takeIt();
                    return TokenKind.NEQUAL;
                }
                else {
                    return (TokenKind.EXCLAMATION);
                }
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
            case ',':
                takeIt();
                return (TokenKind.COMMA);
            case '\t':
            case '\r':
            case '\n':
            case ' ':
            case '\\':
                skipIt();
                return scanToken(false);
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
                        if (!isDigit(currentChar)){
                            currentSpelling.append('0');
                            return (TokenKind.NUM);
                        }
                        continue;
                    } else {
                        leadingZero = false;
                        takeIt();
                    }
                }
                return (TokenKind.NUM);
        }
        if (comment){
            skipIt();
            return scanToken(true);
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
