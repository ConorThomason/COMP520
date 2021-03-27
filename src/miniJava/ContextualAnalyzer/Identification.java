package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.TokenKind;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;

public class Identification implements Visitor<Object, Object> {
   final static boolean debug = true;

    public IdentificationTable table;
    private ErrorReporter reporter;

    public BaseType errorType = new BaseType(TypeKind.ERROR, null);
    public BaseType unsupportedType = new BaseType(TypeKind.UNSUPPORTED, null);

    public Identification(Package ast, ErrorReporter reporter){
        this.reporter = reporter;
        table = new IdentificationTable(reporter);

        if (debug) System.out.println("Entering visitPackage");
        visitPackage(ast, table);
    }

    @Override
    public Object visitPackage(Package prog, Object arg) {
        table.openScope();

        //Add classes to table
        for (ClassDecl cd : prog.classDeclList){
            if (debug) System.out.println("Attempting to insert " + cd.name + ", " + cd);
            table.insert(cd.name, cd);
        }
        //Then visit

        for (ClassDecl cd : prog.classDeclList){
            if (debug) System.out.println("Attempting to visit " + cd);
            cd.visit(this, arg);
        }

        table.closeScope();
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {

        for (MethodDecl m: cd.methodDeclList){
            if (debug) System.out.println("Attempting to insert " + m.name + ", " + m);
            table.insert(m.name, m);
        }

        for (MethodDecl m: cd.methodDeclList){
            table.openScope();
            if (debug) System.out.println("Attempting to visit " + m);
            m.visit(this, arg);
            table.closeScope();
        }
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        if (debug) System.out.println("Attempting to visit " + fd);
        fd.type.visit(this, arg);
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        for (ParameterDecl pd : md.parameterDeclList){
            if (debug) System.out.println("Attempting to insert " + pd.name + ", " + pd);
        }

        for (Statement s: md.statementList){
            if (debug) System.out.println("Attempting to insert " + s);
        }

        for (ParameterDecl pd: md.parameterDeclList){
            if (debug) System.out.println("Attempting to visit " + pd);
            pd.visit(this, arg);
        }
        table.openScope();

        for (Statement s: md.statementList){
            if (debug) System.out.println("Attempting to visit " + s);
            s.visit(this, arg);
        }
        if (md.type.typeKind != TypeKind.NULL){
            if (debug) System.out.println("Attempting to visit " + md);
            md.type.visit(this, arg);
        }

        table.closeScope();
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        if (debug) System.out.println("Attempting to visit " + pd);
        pd.type.visit(this, arg);
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        if (debug) System.out.println("Attempting to visit " + decl);
        decl.type.visit(this, arg);
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        for (Statement s: stmt.sl){
            if (debug) System.out.println("Attempting to visit " + stmt);
            s.visit(this, "");
        }
        return null;
    }
    public boolean errorUnsupportedCheck(TypeDenoter given){
       if (!given.equals(errorType) || !given.equals(unsupportedType)){
           return true;
       }
       return false;
    }

    public boolean errorUnsupportedCheck(TypeDenoter given1, TypeDenoter given2){
        if (!given1.equals(errorType) || !given1.equals(unsupportedType) && (!given2.equals(errorType) ||
                !given2.equals(unsupportedType))){
            return true;
        }
        return false;
    }
    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        if (debug) System.out.println("Attempting to visit " + stmt);
        TypeDenoter expressionType = (TypeDenoter) stmt.initExp.visit(this, arg);
        if (debug) System.out.println("expressionType: " + expressionType);
        if (errorUnsupportedCheck(expressionType)){
            if(stmt.varDecl.type != expressionType){
                System.out.println("Type Mismatch - VarDeclStmt");
                System.exit(4);
            }
        }
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        if (debug) System.out.println("Attempting to visit " + stmt);
        TypeDenoter referenceType = (TypeDenoter) stmt.ref.visit(this, arg);
        if (debug) System.out.println("Attempting to visit " + stmt);
        TypeDenoter valueType = (TypeDenoter) stmt.val.visit(this, null);
        if (errorUnsupportedCheck(referenceType, valueType))
            if(valueType != referenceType){
                System.out.println("Type Mismatch - AssignStmt");
                System.exit(4);
            }
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        ExprList argumentList = stmt.argList;
        for (Expression e : argumentList){
            if (debug) System.out.println("Attempting to visit " + e);
            TypeDenoter expressionType = (TypeDenoter) e.visit(this, arg);
            if (expressionType.typeKind == TypeKind.ERROR || expressionType.typeKind ==
            TypeKind.UNSUPPORTED) return null;
        }

        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        if (debug) System.out.println("Attempting to visit " + stmt);
        TypeDenoter conditionType = (TypeDenoter) stmt.cond.visit(this, arg);
        if (errorUnsupportedCheck(conditionType)){
            System.out.println("IfStmt - Condition type not boolean");
            System.exit(4);
        }
        if (debug) System.out.println("Attempting to visit " + stmt);
        stmt.thenStmt.visit(this, arg);
        if (stmt.elseStmt != null)
            if (debug) System.out.println("Attempting to visit " + stmt);
            stmt.elseStmt.visit(this, arg);
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        TypeDenoter returnedType = null;
        if (debug) System.out.println("Attempting to visit " + expr);
        expr.operator.visit(this, arg);
        TypeDenoter internalType = (TypeDenoter) expr.expr.visit(this, arg);
        if (errorUnsupportedCheck(internalType)){
           returnedType = new BaseType(TypeKind.ERROR, null);
        }
        else{
            switch (expr.operator.kind){
                case MINUS:
                    if (internalType.typeKind == TypeKind.INT){
                        System.out.println("Unary Expr Int Numerical Negation Error");
                        returnedType = new BaseType(TypeKind.UNSUPPORTED, null);
                    } else{
                        returnedType = internalType;
                    }
                    break;
                case EXCLAMATION:
                    if (internalType.typeKind != TypeKind.BOOLEAN){
                        System.out.println("Unary Expr Boolean Logical Negation Error");
                        returnedType = new BaseType(TypeKind.UNSUPPORTED, null);
                    } else{
                        returnedType = internalType;
                    }
                    break;
                default:
                    System.out.println("Invalid unary op");
                    returnedType = new BaseType(TypeKind.UNSUPPORTED, null);
                    break;
            }
        }
        //TODO expr.astType = ret;
        return returnedType;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        TypeDenoter returnedType = null;
        expr.operator.visit(this, arg);
        TypeDenoter left = (TypeDenoter) expr.left.visit(this, arg);
        TypeDenoter right = (TypeDenoter) expr.right.visit(this, arg);

        if (errorUnsupportedCheck(left, right)){
            returnedType = new BaseType(TypeKind.ERROR, null);
        }
        else{
            returnedType = (TypeDenoter) binaryCheck(expr, left, right, expr.operator);
        }
        return returnedType;
    }

    public Object binaryCheck(BinaryExpr expr, TypeDenoter left, TypeDenoter right,
                              Operator operator){
        TypeDenoter returnedType = null;
        switch(operator.kind){
            case PLUS:
            case MINUS:
            case TIMES:
            case DIVIDE:
            case AND:
            case OR:
                returnedType = (TypeDenoter) binaryCheckSimplified(expr, left, right, operator, true);
                break;
            case EQUALS:
            case NEQUAL:
            case GTHAN:
            case GEQUAL:
            case LTHAN:
            case LEQUAL:
                returnedType = (TypeDenoter) binaryCheckSimplified(expr, left, right, operator, false);
                break;
            default:
                System.out.println("Unsupported binary operator");
                returnedType = new BaseType(TypeKind.UNSUPPORTED, null);
        }
        return returnedType;
    }

    public Object binaryCheckSimplified(BinaryExpr expr, TypeDenoter left, TypeDenoter right,
                                        Operator operator, boolean trueIfLeft){
        TypeDenoter returnedType = null;
        if (left.typeKind!= TypeKind.INT || right.typeKind != TypeKind.INT){
            System.out.println("Type mismatch " + operator.kind + " binary expression");
            returnedType = new BaseType(left.typeKind, null);
        }
        return returnedType;
    }
    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        TypeDenoter returnedType = null;
        TypeDenoter internalType = (TypeDenoter) expr.ref.visit(this, arg);

        if (internalType != null && (errorUnsupportedCheck(internalType))){
            returnedType = new BaseType(TypeKind.ERROR, null);
        }
        else{
            returnedType = internalType;
        }
        return returnedType;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        TypeDenoter returnedType = null;
        TypeDenoter functionType = (TypeDenoter) expr.functionRef.visit(this, arg);
        if (errorUnsupportedCheck(functionType)){
            returnedType = new BaseType(TypeKind.ERROR, null);
        }
        else{
            returnedType = functionType;
            ExprList argumentList = expr.argList;
            MethodDecl castDeclaration = (MethodDecl) expr.functionRef.declaration;
            if (castDeclaration.parameterDeclList.size() != argumentList.size()){
                returnedType = new BaseType(TypeKind.UNSUPPORTED, null);
                System.out.println("Function call error, incorrect number of arguments");
            }
            Iterator<ParameterDecl> parameterListIterator = castDeclaration.parameterDeclList.iterator();
            Iterator<Expression> argumentIterator = argumentList.iterator();


            while (parameterListIterator.hasNext()){
                ParameterDecl parameterDecl = parameterListIterator.next();
                Expression expression = argumentIterator.next();
                TypeDenoter argumentType = (TypeDenoter) expression.visit(this, arg);
                if (argumentType != parameterDecl.type){
                    returnedType = new BaseType(TypeKind.ERROR, null);
                    System.out.println("Function call argument does not match declared type");
                }
            }
        }

        return returnedType;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        TypeDenoter returnedType = null;
        TypeDenoter literalType = (TypeDenoter) expr.lit.visit(this, arg);
        if (errorUnsupportedCheck(literalType)){
            returnedType = errorType;
        } else{
            returnedType = literalType;
        }
        return returnedType;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        return expr.classtype;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        TypeDenoter returnedType = null;
        TypeDenoter sizeType = (TypeDenoter) expr.sizeExpr.visit(this, arg);
        if (errorUnsupportedCheck(sizeType)){
            returnedType = errorType;
        } else{
            TypeDenoter arrayType = expr.eltType;
            if (!sizeType.equals(new BaseType(TypeKind.INT, null))){
                returnedType = errorType;
                System.out.println("Array requires an int size");
            }
        }
        return returnedType;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return id.declaration.type;
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        return new BaseType(TypeKind.INT, null);
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return new BaseType(TypeKind.BOOLEAN, null);
    }

}
