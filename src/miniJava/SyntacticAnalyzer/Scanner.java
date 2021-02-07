package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

public class Scanner {
    private InputStream inputStream;
    private char currentChar;
    private StringBuilder currentSpelling;

    private boolean eot = false;

    public Scanner(InputStream inputStream){
        this.inputStream = inputStream;
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
       return (c >= '0' && (c <= '9');
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
