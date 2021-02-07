package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

public class Scanner {
    private InputStream inputStream;
    private ErrorReporter reporter;
    private char currentChar;
    private StringBuilder currentSpelling;

    private boolean eot = false;

    public Scanner(InputStream inputStream, ErrorReporter reporter){
        this.inputStream = inputStream;
        this.reporter = reporter;
        readChar();
    }
    public Token scan(){
        while (!eot && currentChar == ' ')
            skipIt();

        //token starts, get spelling and identify token kind
        currentSpelling = new StringBuilder();
        TokenKind kind = scanToken();
        String spelling = currentSpelling.toString();

        return new Token(kind, spelling);
    }


   /*Determine token kind*/
   public TokenKind scanToken() {
       if (eot) {
           return TokenKind.EOT;
       }

       //Scan token
       switch(currentChar){
           case '+':
               takeIt();
               return(TokenKind.PLUS);
           case '-':
               takeIt();
               return(TokenKind.MINUS);
           case '*':
               takeIt();
               return(TokenKind.TIMES);
           case '/':
               takeIt();
               return(TokenKind.DIVIDE);
           case '[':
               takeIt();
               return(TokenKind.LSQUARE);
           case ']':
               takeIt();
               return(TokenKind.RSQUARE);
           case '{':
               takeIt();
               return(TokenKind.LCURLY);
           case '}':
               takeIt();
               return(TokenKind.RCURLY);
           case '(':
               takeIt();
               return(TokenKind.RPAREN);
           case ')':
               takeIt();
               return(TokenKind.RPAREN);
           case '0': case '1': case '2': case '3': case '4':
           case '5': case '6': case '7': case '8': case '9':
               while(isDigit(currentChar))
                   takeIt();
               return(TokenKind.NUM);

           default:
               scanError("Unrecognized character '" + currentChar + "' in input");
               return(TokenKind.ERROR);

       }
   }

   private void takeIt(){
       currentSpelling.append(currentChar);
       nextChar();
   }

   private void skipIt(){
       nextChar();
   }

   private boolean isDigit(char c){
       return (c >= '0') && (c <= '9');
   }

   private void scanError(String m){
       reporter.reportError("Scan Error: " + m);
   }

   private final static char eolUnix = '\n';
   private final static char eolWindows ='\r';

   private void nextChar(){
       if (!eot)
           readChar();
   }

   private void readChar(){
       try{
           int c = inputStream.read();
           currentChar = (char) c;
           if (c == -1 || currentChar == eolUnix || currentChar == eolWindows)
               eot = true;
       } catch (IOException e) {
//           scanError("I/O Exception"); TODO
           eot = true;
       }
   }
}
