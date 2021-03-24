package miniJava.SyntacticAnalyzer;

public class Token {
    public TokenKind kind;
    public String spelling;
    public SourcePosition position;

    public Token(TokenKind kind, String spelling, SourcePosition position){
        this.kind = kind;
        this.spelling = spelling;
        this.position = position;
    }
}
