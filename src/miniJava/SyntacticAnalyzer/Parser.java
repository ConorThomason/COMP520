package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import sun.java2d.pipe.SpanShapeRenderer;

import java.lang.reflect.Parameter;

public class Parser {

    private Scanner scanner;
    private ErrorReporter reporter;
    private Token token;
    private boolean trace = true;

    public Parser(Scanner scanner, ErrorReporter reporter) {
        this.scanner = scanner;
        this.reporter = reporter;
    }


    /* SyntaxError - unwind parse stack when parse fails*/
    class SyntaxError extends Error {
        private static final long serialVersionUID = 1L;
    }

    public Package parse() {
        token = scanner.scan();
        try {
            return parsePackage();
        } catch (SyntaxError e) {
            return null;
        }
    }

    public Package parsePackage() throws SyntaxError {
        Package packageAST = null;

        ClassDeclList classDeclListAST = new ClassDeclList();

        while (token.kind == TokenKind.CLASS) {
            ClassDecl classDeclAST = parseClassDeclaration();
            classDeclListAST.add(classDeclAST);
        }
        packageAST = new Package(classDeclListAST, null);
        if (token.kind != TokenKind.EOT) {
            parseError("Package parse error");
        }
        accept(TokenKind.EOT);
        return packageAST;
    }

    private ClassDecl parseClassDeclaration() {
        ClassDecl classDeclAST = null;
        FieldDeclList fieldList = new FieldDeclList();
        MethodDeclList methodList = new MethodDeclList();
        String className;
        accept(TokenKind.CLASS);
        className = token.spelling;
        Identifier idAST = new Identifier(token);
        accept(TokenKind.ID);

        accept(TokenKind.LCURLY);
        classDeclAST = parseFieldMethodDeclaration(fieldList, methodList, className);
        return classDeclAST;
    }

    //single ::= (Type | Void) id ( ; | ((param?) {statement*})
    private ClassDecl parseFieldMethodDeclaration(FieldDeclList fieldList, MethodDeclList methodList, String className) {

        FieldDecl fieldDecl = null;
        MethodDecl methodDecl = null;
        while (token.kind != TokenKind.RCURLY) {
            Boolean isPrivate = null;
            if (token.kind == TokenKind.PUBLIC || token.kind == TokenKind.PRIVATE) isPrivate = parseVisibility();
            Boolean isStatic = null;
            if (token.kind == TokenKind.STATIC){
                isStatic = parseAccess();
            }

            TypeDenoter typeDenoter = null;
            String name;
            ParameterDeclList params = new ParameterDeclList();
            StatementList statementList = new StatementList();
            if (token.kind == TokenKind.VOID) {
                typeDenoter = new BaseType(TypeKind.VOID, null);
                acceptIt();
                name = token.spelling;
                accept(TokenKind.ID);
                accept(TokenKind.LPAREN);
                if (token.kind != TokenKind.RPAREN) {
                    params = parseParameterList();
                }
                accept(TokenKind.RPAREN);
                accept(TokenKind.LCURLY);
                while (token.kind != TokenKind.RCURLY) {
                    statementList.add(parseStatement());
                }
                accept(TokenKind.RCURLY);
                MemberDecl memberInstance = new FieldDecl((isPrivate != null) ? isPrivate : false,
                        (isStatic != null) ? isStatic : false, typeDenoter, name, null);
                MethodDecl methodInstance = new MethodDecl(memberInstance, params, statementList, null);
                methodList.add(methodInstance);
            } else {
                typeDenoter = parseType();
                name = token.spelling;
                accept(TokenKind.ID);
                if (token.kind == TokenKind.SEMICOL) {
                    //BaseType here is iffy, could also be ClassType, if autograder complains, check this
                    FieldDecl fieldInstance = new FieldDecl((isPrivate != null) ? isPrivate : false,
                            (isStatic != null) ? isStatic : false, typeDenoter,
                            name, null);
                    fieldList.add(fieldInstance);
                    acceptIt();
                } else if (token.kind == TokenKind.LPAREN) {
                    acceptIt();
                    if (token.kind != TokenKind.RPAREN) {
                        params = parseParameterList();
                    }
                    accept(TokenKind.RPAREN);
                    accept(TokenKind.LCURLY);
                    while (token.kind != TokenKind.RCURLY) {
                        statementList.add(parseStatement());
                    }
                    MemberDecl memberInstance = new FieldDecl((isPrivate != null) ? isPrivate : false,
                            (isStatic != null) ? isStatic : false, typeDenoter, name, null);
                    MethodDecl methodInstance = new MethodDecl(memberInstance, params, statementList, null);
                    methodList.add(methodInstance);
                    acceptIt();
                } else {
                    parseError("Field/Method declaration error");
                }
            }
        }
        acceptIt();
        return new ClassDecl(className, fieldList, methodList, null);

    }

    private boolean referenceCheck() {
        if (token.kind == TokenKind.ID || token.kind == TokenKind.THIS) {
            return true;
        }
        return false;
    }

    private Statement parseStatement() {
        //Reference Section
        if (token.kind == TokenKind.LCURLY) {
            acceptIt();
            StatementList statementList = new StatementList();
            while (token.kind != TokenKind.RCURLY) {
                statementList.add(parseStatement());
            }
            accept(TokenKind.RCURLY);
            return new BlockStmt(statementList, null);
        } else if (token.kind == TokenKind.BOOLEAN) {
            acceptIt();
            VarDecl varBool = new VarDecl(new BaseType(TypeKind.BOOLEAN, null), token.spelling, null);
            accept(TokenKind.ID);
            accept(TokenKind.EQUALS);
            Expression expression = parseExpression();
            accept(TokenKind.SEMICOL);
            return new VarDeclStmt(varBool, expression, null);
        } else if (token.kind == TokenKind.RETURN) {
            acceptIt();
            Expression expression = null;
            if (token.kind != TokenKind.SEMICOL) {
                expression = parseExpression();
            }
            accept(TokenKind.SEMICOL);
            return new ReturnStmt(expression, null);
        } else if (token.kind == TokenKind.IF) {
            acceptIt();
            accept(TokenKind.LPAREN);
            Expression expression = parseExpression();
            accept(TokenKind.RPAREN);
            Statement statement = parseStatement();
            if (token.kind == TokenKind.ELSE) {
                acceptIt();
                Statement statement2 = parseStatement();
                return new IfStmt(expression, statement, statement2, null);
            }
            return new IfStmt(expression, statement, null);
        } else if (token.kind == TokenKind.WHILE) {
            acceptIt();
            accept(TokenKind.LPAREN);
            Expression expression = parseExpression();
            accept(TokenKind.RPAREN);
            Statement statement = parseStatement();
            return new WhileStmt(expression, statement, null);
        } else if (token.kind == TokenKind.THIS) {
            acceptIt();
            Reference reference = new ThisRef(null);
            while (token.kind == TokenKind.PERIOD) {
                acceptIt();
                reference = new QualRef(reference, new Identifier(token), null);
                accept(TokenKind.ID);
            }
            if (token.kind == TokenKind.EQUALS) {
                acceptIt();
                Expression expression = parseExpression();
                accept(TokenKind.SEMICOL);
                return new AssignStmt(reference, expression, null);
            } else if (token.kind == TokenKind.LPAREN) {
                acceptIt();
                ExprList expressionList = new ExprList();
                if (token.kind != TokenKind.RPAREN) {
                    expressionList = parseArgumentList();
                }
                accept(TokenKind.RPAREN);
                accept(TokenKind.SEMICOL);
                return new CallStmt(reference, expressionList, null);
            } else if (token.kind == TokenKind.LSQUARE){
                acceptIt();
                Expression expression = parseExpression();
                accept(TokenKind.RSQUARE);
                accept(TokenKind.EQUALS);
                Expression expression2 = parseExpression();
                accept(TokenKind.SEMICOL);
                return new IxAssignStmt(reference, expression, expression2, null);
            }
            else {
                parseError("Statement error - this");
            }
        } else if (token.kind == TokenKind.INT) {
            acceptIt();
            TypeDenoter type = null;
            if (token.kind == TokenKind.LSQUARE) {
                acceptIt();
                accept(TokenKind.RSQUARE);
                type = new ArrayType(new BaseType(TypeKind.INT, null), null);
            } else {
                type = new BaseType(TypeKind.INT, null);
            }
            VarDecl var = new VarDecl(type, token.spelling, null);
            accept(TokenKind.ID);
            accept(TokenKind.EQUALS);
            Expression expression = parseExpression();
            accept(TokenKind.SEMICOL);
            return new VarDeclStmt(var, expression, null);
        } else if (token.kind == TokenKind.ID) {
            Token id = token;
            acceptIt();

            if (token.kind == TokenKind.ID) {
                TypeDenoter type = new ClassType(new Identifier(id), null);
                VarDecl var = new VarDecl(type, token.spelling, null);
                acceptIt();
                accept(TokenKind.EQUALS);
                Expression expression = parseExpression();
                accept(TokenKind.SEMICOL);
                return new VarDeclStmt(var, expression, null);
            } else if (token.kind == TokenKind.LSQUARE) {
                acceptIt();
                TypeDenoter type = null;
                if (token.kind == TokenKind.RSQUARE) {
                    acceptIt();
                    type = new ArrayType(new ClassType(new Identifier(id), null), null);
                    VarDecl var = new VarDecl(type, token.spelling, null);
                    accept(TokenKind.ID);
                    accept(TokenKind.EQUALS);
                    Expression expression = parseExpression();
                    accept(TokenKind.SEMICOL);
                    return new VarDeclStmt(var, expression, null);
                }
                IdRef assignExp = new IdRef(new Identifier(id), null);
                Expression expression = parseExpression();
                IxExpr ref = new IxExpr(assignExp, expression, null);
                accept(TokenKind.RSQUARE);
                accept(TokenKind.EQUALS);
                Expression expression1 = parseExpression();
                accept(TokenKind.SEMICOL);
                return new IxAssignStmt(assignExp, expression, expression1, null);
            } else {
                Reference idRef = new IdRef(new Identifier(id), null);
                while (token.kind == TokenKind.PERIOD) {
                    acceptIt();
                    idRef = new QualRef(idRef, new Identifier(token), null);
                    accept(TokenKind.ID);
                }
                if (token.kind == TokenKind.EQUALS) {
                    acceptIt();
                    Expression expression = parseExpression();
                    accept(TokenKind.SEMICOL);
                    return new AssignStmt(idRef, expression, null);
                } else if (token.kind == TokenKind.LPAREN) {
                    acceptIt();
                    ExprList expressionList = new ExprList();
                    if (token.kind != TokenKind.RPAREN) {
                        expressionList = parseArgumentList();
                    }
                    accept(TokenKind.RPAREN);
                    accept(TokenKind.SEMICOL);
                    return new CallStmt(idRef, expressionList, null);
                } else if (token.kind == TokenKind.LSQUARE){
                    acceptIt();
                    Expression expression = parseExpression();
                    accept(TokenKind.RSQUARE);
                    accept(TokenKind.EQUALS);
                    Expression expression2 = parseExpression();
                    accept(TokenKind.SEMICOL);
                    return new IxAssignStmt(idRef, expression, expression2, null);
                }
                else {
                    parseError("Statement error - ID");
                    return null;
                }
            }
        }
        parseError("Statement error - dropped off end");
        return null;
    }

    private boolean checkIfArgumentList() {
        TokenKind kind = token.kind;
        return kind == TokenKind.THIS || kind == TokenKind.ID || kind == TokenKind.EXCLAMATION ||
                kind == TokenKind.MINUS || kind == TokenKind.LPAREN || kind == TokenKind.NUM ||
                kind == TokenKind.TRUE || kind == TokenKind.FALSE || kind == TokenKind.NEW;
    }

    //Because of the fact we want to implement precedence, I am forced to divide them up one into another, and call
    // as the precedence suggests.
    //...yay?
    private Expression parseExpression() {
        return parseOr();
    }

    private Expression parseOr() {
        Expression or = parseAnd();
        while (token.kind == TokenKind.OR) {
            Token temp = token;
            acceptIt();
            or = new BinaryExpr(new Operator(temp), or, parseAnd(), null);
        }
        return or;
    }

    private Expression parseAnd() {
        Expression and = parseEq();
        while (token.kind == TokenKind.AND) {
            Token temp = token;
            acceptIt();
            and = new BinaryExpr(new Operator(temp), and, parseEq(), null);
        }
        return and;
    }

    private Expression parseEq() {
        Expression eq = parseIneq();
        while (token.kind == TokenKind.EQUIVALENT || token.kind == TokenKind.NEQUAL) {
            Token temp = token;
            acceptIt();
            eq = new BinaryExpr(new Operator(temp), eq, parseIneq(), null);
        }
        return eq;
    }

    private Expression parseIneq() {
        Expression ineq = parseAdd();
        while (token.kind == TokenKind.LEQUAL || token.kind == TokenKind.LTHAN || token.kind == TokenKind.GTHAN ||
                token.kind == TokenKind.GEQUAL) {
            Token temp = token;
            acceptIt();
            ineq = new BinaryExpr(new Operator(temp), ineq, parseAdd(), null);
        }
        return ineq;
    }

    private Expression parseAdd() {
        Expression add = parseMult();
        while (token.kind == TokenKind.PLUS || token.kind == TokenKind.MINUS) {
            Token temp = token;
            acceptIt();
            add = new BinaryExpr(new Operator(temp), add, parseMult(), null);
        }
        return add;
    }

    private Expression parseMult() {
        Expression mult = parseUnary();
        while (token.kind == TokenKind.TIMES || token.kind == TokenKind.DIVIDE) {
            Token temp = token;
            acceptIt();
            mult = new BinaryExpr(new Operator(temp), mult, parseUnary(), null);
        }
        return mult;
    }

    private Expression parseUnary() {
        Expression unary;
        if (token.kind == TokenKind.MINUS || token.kind == TokenKind.EXCLAMATION) {
            Operator unaryOperator = new Operator(token);
            acceptIt();
            unary = new UnaryExpr(unaryOperator, parseUnary(), null);
        } else unary = parseRemaining();
        return unary;
    }

    private Expression parseRemaining() {
        Expression expression1 = null;

        if (token.kind == TokenKind.NUM) {
            expression1 = new LiteralExpr(new IntLiteral(token), null);
            acceptIt();
        } else if (token.kind == TokenKind.TRUE || token.kind == TokenKind.FALSE) {
            expression1 = new LiteralExpr(new BooleanLiteral(token), null);
            acceptIt();
        } else if (token.kind == TokenKind.LPAREN) {
            acceptIt();
            expression1 = parseExpression();
            accept(TokenKind.RPAREN);
        } else if (token.kind == TokenKind.NEW) {
            while(true) {
                acceptIt();
                if (token.kind == TokenKind.INT) {
                    acceptIt();
                    accept(TokenKind.LSQUARE);
                    expression1 = new NewArrayExpr(new BaseType(TypeKind.INT, null), parseExpression(), null);
                    accept(TokenKind.RSQUARE);
                    break;
                } else if (token.kind == TokenKind.ID) {
                    Token newId = token;
                    accept(TokenKind.ID);
                    if (token.kind == TokenKind.LPAREN) {
                        acceptIt();
                        accept(TokenKind.RPAREN);
                        expression1 = new NewObjectExpr(new ClassType(new Identifier(newId), null), null);
                        break;
                    } else {
                        accept(TokenKind.LSQUARE);
                        expression1 = new NewArrayExpr(new ClassType(new Identifier(newId), null), parseExpression(),
                                null);
                        accept(TokenKind.RSQUARE);
                        break;
                    }
                } else if (token.kind == TokenKind.EOT){
                    break;
                }
            }
        } else if (token.kind == TokenKind.THIS) {
            while (true) {
                acceptIt();
                Reference thisRoot = new ThisRef(null);
                while (token.kind == TokenKind.PERIOD) {
                    acceptIt();
                    thisRoot = new QualRef(thisRoot, new Identifier(token), null);
                    accept(TokenKind.ID);
                }
                if (token.kind == TokenKind.LSQUARE){
                    acceptIt();
                    expression1 = new IxExpr(thisRoot, parseExpression(), null);
                    accept(TokenKind.RSQUARE);
                    break;
                }
                if (token.kind != TokenKind.LPAREN) {
                    expression1 = new RefExpr(thisRoot, null);
                    break;
                }
                accept(TokenKind.LPAREN);
                if (token.kind != TokenKind.RPAREN) {
                    expression1 = new CallExpr(thisRoot, parseArgumentList(), null);
                } else {
                    expression1 = new CallExpr(thisRoot, new ExprList(), null);
                }
                accept(TokenKind.RPAREN);
                break;
            }
        } else if (token.kind == TokenKind.ID) {
            while (true) {
                Reference idRoot = new IdRef(new Identifier(token), null);
                IdRef temp = (IdRef) idRoot;
                acceptIt();
                if (token.kind == TokenKind.LSQUARE) {
                    acceptIt();
                    //Reference, Expression, Source Pos
                    expression1 = new IxExpr(temp, parseExpression(), null);
                    accept(TokenKind.RSQUARE);
                    break;
                }
                while (token.kind == TokenKind.PERIOD) {
                    acceptIt();
                    idRoot = new QualRef(idRoot, new Identifier(token), null);
                    accept(TokenKind.ID);
                }
                if (token.kind != TokenKind.LPAREN) {
                    if (token.kind == TokenKind.LSQUARE){
                        acceptIt();
                        expression1 = parseExpression();
                        accept(TokenKind.RSQUARE);
                        expression1 = new IxExpr(idRoot, expression1, null);
                        break;
                    }
                    expression1 = new RefExpr(idRoot, null);
                    break;
                }
                accept(TokenKind.LPAREN);
                ExprList expressionList = new ExprList();
                if (token.kind != TokenKind.RPAREN) {
                    expressionList = parseArgumentList();
                }
                expression1 = new CallExpr(idRoot, expressionList, null);
                accept(TokenKind.RPAREN);
                break;
            }
        } else {
            parseError("Expression parse error");
            return null;
        }
        return expression1;
    }

    private boolean checkExpression() {
        if (token.kind == TokenKind.ID || token.kind == TokenKind.THIS || token.kind == TokenKind.NUM ||
                token.kind == TokenKind.TRUE || token.kind == TokenKind.FALSE || token.kind == TokenKind.NEW) {
            return true;
        }
        return false;
    }

    //Reference -> id Reference' | this Reference'
    private Reference parseReference() {
        Token savedToken = token;
        Reference root = null;
        if (token.kind == TokenKind.THIS || token.kind == TokenKind.ID) {
            if (token.kind == TokenKind.THIS) {
                acceptIt();
                root = new ThisRef(null);
            } else {
                acceptIt();
                root = new IdRef(new Identifier(savedToken), null);
            }
        }
        while (token.kind == TokenKind.PERIOD) {
            acceptIt();
            root = new QualRef(root, new Identifier(savedToken), null);
            accept(TokenKind.ID);
        }
        return root;
    }

    private ExprList parseArgumentList() {
        ExprList expressionList = new ExprList();
        expressionList.add(parseExpression());
        while (token.kind == TokenKind.COMMA) {
            acceptIt();
            expressionList.add(parseExpression());
        }
        return expressionList;
    }

    private ParameterDeclList parseParameterList() {
        ParameterDeclList result = new ParameterDeclList();
        while (couldBeType()) {
            if (couldBeType()) {
                result.add(new ParameterDecl(parseType(), token.spelling, null));
                accept(TokenKind.ID);
                if (token.kind == TokenKind.COMMA) {
                    acceptIt();
                }
            }
            else {
                parseError("Parameter list error");
            }
        }
        return result;
    }

    private TypeDenoter parseType() {
        if (token.kind == TokenKind.BOOLEAN) {
            acceptIt();
            return new BaseType(TypeKind.BOOLEAN, null);
        } else if (token.kind == TokenKind.INT) {
            accept(TokenKind.INT);
            if (token.kind == TokenKind.LSQUARE) {
                acceptIt();
                accept(TokenKind.RSQUARE);
                return new ArrayType(new BaseType(TypeKind.INT, null), null);
            }
            return new BaseType(TypeKind.INT, null);
        } else if (token.kind == TokenKind.ID) {
            String typeName = token.spelling;
            Token prevToken = token;
            accept(TokenKind.ID);
            if (token.kind == TokenKind.LSQUARE) {
                acceptIt();
                accept(TokenKind.RSQUARE);
                return new ArrayType(new ClassType(new Identifier(prevToken), null), null);
            }
            return new ClassType(new Identifier(prevToken), null);
        } else {
            parseError("Type Error");
        }
        return new BaseType(TypeKind.UNSUPPORTED, null);
    }

    private boolean couldBeType() {
        if (token.kind == TokenKind.BOOLEAN || token.kind == TokenKind.INT || token.kind == TokenKind.ID) {
            return true;
        }
        return false;
    }

    private boolean parseAccess() {
        if (token.kind == TokenKind.STATIC) {
            acceptIt();
            return true;
        }
        return false;
    }

    private boolean parseVisibility() {
        if (token.kind == TokenKind.PUBLIC) {
            acceptIt();
            return false;
        } else {
            acceptIt();
            return true;
        }
    }

    private void acceptIt() throws SyntaxError {
        accept(token.kind);
    }

    /* Verify that the current token in inp0ut matches expected token and advance to next token
     */
    private void accept(TokenKind expectedTokenKind) throws SyntaxError {
        if (token.kind == expectedTokenKind || (token.kind.toSimple() == expectedTokenKind.toSimple() &&
                token.kind.toSimple() != null)) {
            if (trace)
                pTrace();
            token = scanner.scan();
        } else
            parseError("expecting '" + expectedTokenKind + "' but found '" + token.kind + "'");
    }

    /* report parse error, unwind call stack to start of parse*/
    private void parseError(String e) throws SyntaxError {
        reporter.reportError("Parse error: " + e);
        throw new SyntaxError();
    }

    /* show parse stack whenever terminal is accepted*/
    private void pTrace() {
        StackTraceElement[] stl = Thread.currentThread().getStackTrace();
        for (int i = stl.length - 1; i > 0; i--) {
            if (stl[i].toString().contains("parse"))
                System.out.println(stl[i]);
        }
        System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
        System.out.println();
    }
}
