package miniJava.SyntacticAnalyzer;

public enum TokenKind {
   ERROR(SimpleToken.BINOP),
   EQUALS(SimpleToken.BINOP),
   PERIOD(null),
   SEMICOL(null),
   MINUS(SimpleToken.BINOP),
   LTHAN(SimpleToken.BINOP),
   GTHAN(SimpleToken.BINOP),
   ANDSYMBOL(SimpleToken.BINOP),
   ORSYMBOL(SimpleToken.BINOP),
   EXCLAMATION(SimpleToken.BINOP),
   PLUS(SimpleToken.BINOP),
   TIMES(SimpleToken.BINOP),
   DIVIDE(SimpleToken.UNOP),
   EOT(null), CLASS(null), PUBLIC(null), PRIVATE(null),
   STATIC(null), ID(null), VOID(null),
   INT(null), BOOLEAN(null), LSQUARE(null),
   RSQUARE(null), LPAREN(null), RPAREN(null),
   LCURLY(null), RCURLY(null), COMMA(null),
   THIS(null), RETURN(null), IF(null), ELSE(null),
   WHILE(null),
   TRUE(null), FALSE(null),NEW(null), NUM(null),
   OTHER(null), COMMENT(null), BLOCKCOMMENT(null), BLOCKCOMMENTEND(null);

   private final SimpleToken simpleToken;

   private TokenKind(SimpleToken simpleToken){
      this.simpleToken = simpleToken;
   }

   public SimpleToken toSimple(){
      return simpleToken;
   }

}