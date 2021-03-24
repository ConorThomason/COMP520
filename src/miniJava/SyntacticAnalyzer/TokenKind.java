package miniJava.SyntacticAnalyzer;

public enum TokenKind {
   ERROR(null),
   EQUALS(null),
   PERIOD(null),
   SEMICOL(null),
   MINUS(SimpleToken.BINOP),
   LTHAN(SimpleToken.BINOP),
   LEQUAL(SimpleToken.BINOP),
   GEQUAL(SimpleToken.BINOP),
   NEQUAL(SimpleToken.BINOP),
   GTHAN(SimpleToken.BINOP),
   AND(SimpleToken.BINOP),
   EQUIVALENT(SimpleToken.BINOP),
   OR(SimpleToken.BINOP),
   EXCLAMATION(SimpleToken.UNOP),
   PLUS(SimpleToken.BINOP),
   TIMES(SimpleToken.BINOP),
   DIVIDE(SimpleToken.BINOP),
   EOT(null), CLASS(null), PUBLIC(null), PRIVATE(null),
   STATIC(null), ID(null), VOID(null),
   INT(null), BOOLEAN(null), LSQUARE(null),
   RSQUARE(null), LPAREN(null), RPAREN(null),
   LCURLY(null), RCURLY(null), COMMA(null),
   THIS(null), RETURN(null), IF(null), ELSE(null),
   WHILE(null),
   TRUE(null), FALSE(null),NEW(null), NUM(null),
   OTHER(null), COMMENT(null), BLOCKCOMMENT(null), BLOCKCOMMENTEND(null),
   NULL(null);

   private final SimpleToken simpleToken;

   private TokenKind(SimpleToken simpleToken){
      this.simpleToken = simpleToken;
   }

   public SimpleToken toSimple(){
      return simpleToken;
   }

}