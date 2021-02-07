package miniJava.SyntacticAnalyzer;

public enum TokenKind {
   EOT, CLASS, PUBLIC, PRIVATE, STATIC, ID, VOID,
   INT, BOOLEAN, LSQUARE, RSQUARE, LPAREN, RPAREN,
   LCURLY, RCURLY, COMMA, THIS, RETURN, IF, ELSE,
   WHILE, UNOP, BINOP, TRUE, FALSE, NEW, NUM, PLUS,
   MINUS, TIMES, DIVIDE;
}