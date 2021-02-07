package miniJava.SyntacticAnalyzer;

public enum TokenKind {
   ERROR, EQUALS, PERIOD, SEMICOL, MINUS,
   LTHAN, GTHAN, ANDSYMBOL, ORSYMBOL, EXCLAMATION {
      @Override
      public TokenKind asSimple() {
         return BINOP;
      }
   }, PLUS, TIMES, DIVIDE{
      @Override
      public TokenKind asSimple(){
         return UNOP;
      }
   },
   EOT, CLASS, PUBLIC, PRIVATE, STATIC, ID, VOID,
   INT, BOOLEAN, LSQUARE, RSQUARE, LPAREN, RPAREN,
   LCURLY, RCURLY, COMMA, THIS, RETURN, IF, ELSE,
   WHILE, UNOP, BINOP, TRUE, FALSE, NEW, NUM, OTHER;
   public TokenKind asSimple(){
      return TokenKind.OTHER;
   }
}