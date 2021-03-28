package miniJava.ContextualAnalyzer;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

import java.util.Iterator;

public class Identification implements Visitor<Object, Object> {
   final static boolean debug = true;

    public IdentificationTable table;
    private ErrorReporter reporter;
    private ClassType workingClass;
    private String forbiddenVariable;

    public BaseType errorType = new BaseType(TypeKind.ERROR, null);
    public BaseType unsupportedType = new BaseType(TypeKind.UNSUPPORTED, null);
    public BaseType nullType = new BaseType(TypeKind.NULL, null);

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
        workingClass = new ClassType(new Identifier(new Token(TokenKind.ID, cd.name, null)), null);
        table.openScope();
        for (MethodDecl m: cd.methodDeclList){
            if (debug) System.out.println("Attempting to insert " + m.name + ", " + m);
            table.insert(m.name, m);
        }
        for (FieldDecl fd: cd.fieldDeclList){
            if (debug) System.out.println("Attempting to insert " + fd.name + ", " + fd);
            table.insert(fd.name, fd);
        }

        for (MethodDecl m: cd.methodDeclList){
            if (debug) System.out.println("Attempting to visit " + m);
            m.visit(this, arg);
        }
        for (FieldDecl fd : cd.fieldDeclList){
            if (debug) System.out.println("Attempting to visit " + fd);
        }
        table.closeScope();
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
        table.openScope();
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
            TypeDenoter type = (TypeDenoter) s.visit(this, arg);
            if (s instanceof ReturnStmt && !type.equals((TypeDenoter) md.type)){
                TypeError("Return statement fails to match function return type", s.posn);
            }
            s.visit(this, arg);
        }
        if (md.type.typeKind != TypeKind.NULL){
            if (debug) System.out.println("Attempting to visit " + md);
            md.type.visit(this, arg);
        }

        table.closeScope();
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
        return decl.type;
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
        if (given.equals(nullType)){
            return true;
        }
       if (!given.equals(errorType) || !given.equals(unsupportedType)){
           return false;
       }
       return true;
    }

    public boolean errorUnsupportedCheck(TypeDenoter given1, TypeDenoter given2){
        if (!given1.equals(errorType) || !given1.equals(unsupportedType) && (!given2.equals(errorType) ||
                !given2.equals(unsupportedType))){
            return false;
        }
        return true;
    }
    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        if (debug) System.out.println("Attempting to visit " + stmt);
        this.forbiddenVariable = stmt.varDecl.name;
        TypeDenoter expressionType = (TypeDenoter) stmt.initExp.visit(this, arg);
        this.forbiddenVariable = null;
        if (debug) System.out.println("expressionType: " + expressionType);
        if (errorUnsupportedCheck(expressionType)){
            if(!stmt.varDecl.type.equals(expressionType)){
                TypeError("Type Mismatch - VarDeclStmt", expressionType.posn);
            }
        }
        if (stmt.initExp instanceof RefExpr){
            RefExpr expression = (RefExpr) stmt.initExp;
            if (expression.ref.declaration instanceof ClassDecl){
               TypeError(expression + " Invalid class assignment", expression.posn);
            }
            if (expression.ref.declaration instanceof MethodDecl){
                TypeError(expression + " Invalid method assignment", expression.posn);
            }
        }
        TypeDenoter reference = (TypeDenoter) stmt.varDecl.visit(this, null);
        TypeDenoter expression = (TypeDenoter) stmt.initExp.visit(this, null);
        if (!reference.equals(expression)){
            if (reference instanceof ClassType && expression instanceof ClassType){
                String name1 = ((ClassType) reference).className.spelling;
                String name2 = ((ClassType) expression).className.spelling;
                if (name1 == null || name2 == null){
                    TypeError("Variable declaration attempts invalid class assignment", reference.posn);
                }
                else{
                    TypeError("Variable declaration attempts assignment of expression to incompatible reference",
                            reference.posn);
                }
            }
            else{
                TypeError("Variable declaration attempts assignment of expression to incompatible reference",
                        reference.posn);
            }
            return new BaseType(TypeKind.ERROR, stmt.posn);
        }
        return reference;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        if (debug) System.out.println("Attempting to visit " + stmt);
        TypeDenoter referenceType = (TypeDenoter) stmt.ref.visit(this, arg);
        if (debug) System.out.println("Attempting to visit " + stmt);
        TypeDenoter valueType = (TypeDenoter) stmt.val.visit(this, null);
        if (errorUnsupportedCheck(referenceType, valueType))
            if(valueType != referenceType){
                TypeError("Type Mismatch - AssignStmt", valueType.posn);
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
        if (stmt.returnExpr != null){
            return stmt.returnExpr.visit(this, null);
        }
        return new BaseType(TypeKind.VOID, stmt.posn);
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        if (debug) System.out.println("Attempting to visit " + stmt);
        TypeDenoter conditionType = (TypeDenoter) stmt.cond.visit(this, arg);
        if (errorUnsupportedCheck(conditionType)){
            TypeError("IfStmt - Condition type not boolean", conditionType.posn);
            System.exit(4);
        }
        if (stmt.thenStmt instanceof VarDeclStmt){
            TypeError("If statement variable declaration not permitted here", stmt.thenStmt.posn);
        }
        if (debug) System.out.println("Attempting to visit " + stmt);
        stmt.thenStmt.visit(this, arg);
        if (stmt.elseStmt != null) {
            if (debug) System.out.println("Attempting to visit " + stmt);
            if (stmt.elseStmt instanceof VarDeclStmt){
                TypeError("If statement variable declaration not permitted here", stmt.elseStmt.posn);
            }
            stmt.elseStmt.visit(this, arg);
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        TypeDenoter conditionType = (TypeDenoter) stmt.cond.visit(this, arg);
        if (errorUnsupportedCheck(conditionType)){
            if (!conditionType.equals(new BaseType(TypeKind.BOOLEAN, null))){
                TypeError("WhileStmt - Condition Type isn't Boolean", stmt.posn);
            }
        }
        if (stmt.body instanceof VarDeclStmt){
            TypeError("Solitary variable declaration not permitted", stmt.posn);
        }
        stmt.body.visit(this, "");
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
                    if (internalType.typeKind != TypeKind.INT){
                        TypeError("Unary Expr Int Numerical Negation Error", internalType.posn);
                        returnedType = new BaseType(TypeKind.UNSUPPORTED, null);
                    } else{
                        returnedType = internalType;
                    }
                    break;
                case EXCLAMATION:
                    if (internalType.typeKind != TypeKind.BOOLEAN){
                        TypeError("Unary Expr Boolean Logical Negation Error", internalType.posn);
                        returnedType = new BaseType(TypeKind.UNSUPPORTED, null);
                    } else{
                        returnedType = internalType;
                    }
                    break;
                default:
                    TypeError("Invalid unary op", internalType.posn);
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
        boolean checkResult = false;
        if (left == null || right == null){
            if (left != null){
                checkResult = errorUnsupportedCheck(left);
            } if (right != null){
                checkResult = errorUnsupportedCheck(right);
            }
        } else{
            checkResult = errorUnsupportedCheck(left, right);
        }
        if (checkResult){
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
            case EQUIVALENT:
            case NEQUAL:
                if (!left.equals(right)){
                    TypeError("Type mismatch, EQ Binary Expression error", expr.posn);
                    returnedType = unsupportedType;
                }
                break;
            case GTHAN:
            case GEQUAL:
            case LTHAN:
            case LEQUAL:
                returnedType = (TypeDenoter) binaryCheckSimplified(expr, left, right, operator, false);
                break;
            default:
                TypeError("Unsupported binary operator", left.posn);
                returnedType = new BaseType(TypeKind.UNSUPPORTED, null);
        }
        return returnedType;
    }

    public Object binaryCheckSimplified(BinaryExpr expr, TypeDenoter left, TypeDenoter right,
                                        Operator operator, boolean trueIfLeft){
        TypeDenoter returnedType = null;
        if (intCheck(left, right) && boolCheck(left, right)){
            TypeError("Type mismatch" + operator.kind + " binary expression error", left.posn);
        } else if (trueIfLeft) {
            returnedType = left;
        } else {
            returnedType = new BaseType(TypeKind.BOOLEAN, null);
        }
        return returnedType;
    }

    public boolean intCheck(TypeDenoter left, TypeDenoter right){
        if (left == null || right == null){
            if (left != null){
                if (!left.equals(new BaseType(TypeKind.INT, null))){
                    return false;
                }
                return true;
            }
            else if (right != null){
                if (!right.equals(new BaseType(TypeKind.INT, null))){
                    return false;
                }
                return true;
            }
        }
        else {
            if (!left.equals(new BaseType(TypeKind.INT, null)) ||
                    !right.equals(new BaseType(TypeKind.INT, null))) {
                return false;
            }
            else {
                return true;
            }
        }
        return false;
    }

    public boolean boolCheck(TypeDenoter left, TypeDenoter right){
        if (!left.equals(new BaseType(TypeKind.BOOLEAN, null)) ||
                !right.equals(new BaseType(TypeKind.BOOLEAN, null))){
            return false;
        }
        return true;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        TypeDenoter returnedType = null;
        if (debug) System.out.println("Attempting to visit " + expr);
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
        expr.ref.visit(this, arg);
        expr.ixExpr.visit(this, arg);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        TypeDenoter returnedType = null;
        if (debug) System.out.println("Attempting to visit " + expr);
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
                TypeError("Function call error, incorrect number of arguments", castDeclaration.posn);
            }
            Iterator<ParameterDecl> parameterListIterator = castDeclaration.parameterDeclList.iterator();
            Iterator<Expression> argumentIterator = argumentList.iterator();


            while (parameterListIterator.hasNext()){
                ParameterDecl parameterDecl = parameterListIterator.next();
                Expression expression = argumentIterator.next();
                if (debug) System.out.println("Attempting to visit " + expression);
                TypeDenoter argumentType = (TypeDenoter) expression.visit(this, arg);
                if (argumentType != parameterDecl.type){
                    returnedType = new BaseType(TypeKind.ERROR, null);
                    TypeError("Function call argument does not match declared type", argumentType.posn);
                }
            }
        }

        return returnedType;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        TypeDenoter returnedType = null;
        if (debug) System.out.println("Attempting to visit " + expr);
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
        if (debug) System.out.println("Attempting to visit " + expr);
        TypeDenoter sizeType = (TypeDenoter) expr.sizeExpr.visit(this, arg);
        if (errorUnsupportedCheck(sizeType)){
            returnedType = errorType;
        } else{
            TypeDenoter arrayType = expr.eltType;
            if (!sizeType.equals(new BaseType(TypeKind.INT, null))){
                returnedType = errorType;
                TypeError("Array requires an int size", sizeType.posn);
            }
        }
        return returnedType;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        return workingClass;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        TypeDenoter returnedType = null;
        if (ref.id.spelling.equals(this.forbiddenVariable)){
            TypeError(ref.id.spelling + " used in illegal position", ref.posn);
        }
        if (debug) System.out.println("Attempting to visit " + ref);
        TypeDenoter idType = (TypeDenoter) ref.id.visit(this, arg);
        if (errorUnsupportedCheck(idType)){
            returnedType = errorType;
        } else{
            returnedType = idType;
        }
        return returnedType;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        return nullType;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        if (id.declaration != null) {
            return id.declaration.type;
        }
        else{
            TypeError(id.spelling + " not declared", id.posn);
            System.exit(4);
        }
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        return new BaseType(TypeKind.UNSUPPORTED, op.posn);
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        return new BaseType(TypeKind.INT, null);
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return new BaseType(TypeKind.BOOLEAN, null);
    }

    @Override
    public Object visitNullLiteral(NullLiteral nullLiteral, Object arg) {
        return new BaseType(TypeKind.NULL, null);
    }

    private void TypeError(String error, SourcePosition pos){
        reporter.reportError("*** line " + pos.start + " ***Type Check error: " + error);
    }

}
