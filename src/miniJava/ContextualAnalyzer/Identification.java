package miniJava.ContextualAnalyzer;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.IntType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;

import java.lang.reflect.Method;
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
    public void insertPredefined(){
        ClassDecl StringDecl = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null);
        StringDecl.type = new BaseType(TypeKind.UNSUPPORTED, null);
        table.insertClass("String", StringDecl);

        MethodDeclList printMethods = new MethodDeclList();
        MemberDecl println = new FieldDecl(false, false, new BaseType(TypeKind.VOID, null),
                "println", null);
        ParameterDeclList printlnParameters = new ParameterDeclList();
        ParameterDecl printlnNParameter = new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null);
        printlnParameters.add(printlnNParameter);
        MethodDecl printlnMethod = new MethodDecl(println, printlnParameters, new StatementList(), null);
        printMethods.add(printlnMethod);
        ClassDecl printStreamDecl = new ClassDecl("_PrintStream", new FieldDeclList(), printMethods, null);
        Identifier printStreamID = new Identifier(new Token
                (TokenKind.ID, "_PrintStream", new SourcePosition()));
        printStreamDecl.type = new ClassType(printStreamID, new SourcePosition());
        table.insertClass("_PrintStream", printStreamDecl);
        table.insert("println", printlnMethod);
        table.insert("n", printlnNParameter);

        Identifier sysId = new Identifier(new Token(TokenKind.ID, "System", new SourcePosition()));
        FieldDeclList sysFields = new FieldDeclList();
        FieldDecl outField = new FieldDecl(false, true, new ClassType(printStreamID, null), "out", null);
        sysFields.add(outField);
        ClassDecl sysDecl = new ClassDecl("System", sysFields, new MethodDeclList(), null);
        sysDecl.type = new ClassType(sysId, new SourcePosition());
        table.insertClass("System", sysDecl);
        table.insert("out", outField);
    }
    @Override
    public Object visitPackage(Package prog, Object arg) {
        table.openScope();
        //Add predefined classes to table
        insertPredefined();
        //Add source classes to table
        for (ClassDecl cd : prog.classDeclList) {
            if (debug) System.out.println("Attempting to insert " + cd.name + ", " + cd);
            for (FieldDecl f : cd.fieldDeclList) {
                table.insert(f.name, f);
            }
            for (MethodDecl md : cd.methodDeclList) {
                table.insert(md.name, md);
            }
            //Then visit
        }

            for (ClassDecl cd : prog.classDeclList) {
                if (debug) System.out.println("Attempting to visit " + cd);
                cd.visit(this, arg);
            }

            table.closeScope();
            return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        //Probably not needed anymore since implementation continuity, but removing it seems like a needless risk?
        workingClass = new ClassType(new Identifier(new Token(TokenKind.ID, cd.name, null)), null);
        table.openScope();
        table.insertClass(cd.name, cd);
        table.openScope();

        for (FieldDecl f : cd.fieldDeclList){
            if (debug) System.out.println("Attempting to visit " + f);
            f.visit(this, arg);
        }
        for (MethodDecl m: cd.methodDeclList){
            if (debug) System.out.println("Attempting to visit " + m);
            m.visit(this, arg);
        }

        table.closeScope();
        table.closeScope();
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        table.insert(fd.name, fd);
        fd.type.visit(this, arg);
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        if (md.type instanceof ClassType){
            if(table.find(((ClassType)md.type).className.spelling) == null){
                TypeError("Method declaration returns DNE", md.posn);
            }
        }
        table.insert(md.name, md);
        table.openScope();

        for (ParameterDecl pd: md.parameterDeclList){
            pd.visit(this, arg);
        }
        table.openScope();
        for (Statement s: md.statementList){
            s.visit(this, arg);
        }

        table.closeScope();
        table.closeScope();
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        table.insert(pd.name, pd);
        pd.type.visit(this, arg);
        if (pd.type instanceof ClassType){
            if (table.find(((ClassType) pd.type).className.spelling) == null){
                TypeError("Undefined class referenced", pd.posn);
            }
        }
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
        if (given == null){
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
            if (expressionType == null){
                TypeDenoter reference = (TypeDenoter) stmt.varDecl.visit(this, null);
                return reference;
            }
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
            boolean skip = false;
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
            else if (reference instanceof ClassType){
                if (debug) System.out.println("Warning: Using class name as variable name.");
            }
            else{
                TypeError("Variable declaration attempts assignment of expression to incompatible reference",
                        reference.posn);
            }
            if (!skip) return new BaseType(TypeKind.ERROR, stmt.posn);
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
        TypeDenoter returnedType = null;
        TypeDenoter expressionType = (TypeDenoter) stmt.ix.visit(this, arg);
        TypeDenoter referenceType = (TypeDenoter) stmt.ref.visit(this, arg);
        if (errorUnsupportedCheck(expressionType, referenceType)){
            returnedType = errorType;
        }
        else if (!expressionType.equals(new BaseType(TypeKind.INT, null))){
            returnedType = errorType;
            TypeError("Index cannot be derived as an integer", expressionType.posn);
        }
        else{
            returnedType = ((ArrayType)referenceType).eltType;
        }
        return returnedType;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        stmt.methodRef.visit(this, arg);
        for (Expression expression : stmt.argList) {
            expression.visit(this, arg);
        }
        if (!(stmt.methodRef.declaration instanceof MethodDecl)){
            TypeError("Illegal call to non-function", stmt.posn);
            return null;
        }
        MethodDecl callMethod = (MethodDecl) stmt.methodRef.declaration;
        if (table.isStaticContext() && !callMethod.isStatic && !(stmt.methodRef instanceof QualRef)){
            TypeError("Static method in illegal context", stmt.posn);
        }
        if (stmt.methodRef instanceof QualRef) {
            QualRef reference = (QualRef) stmt.methodRef;
            if (reference.ref instanceof IdRef) {
                IdRef idReference = (IdRef) reference.ref;
                Declaration idDeclaration = idReference.declaration;
                if (idDeclaration.type instanceof ClassType && idDeclaration instanceof VarDecl) {


                    //TODO Possible issues with class searching.
                    String anotherClassName = ((ClassType) idDeclaration.type).className.spelling;
                    MethodDecl otherClass = (MethodDecl) table.findMethodInClass(anotherClassName, callMethod.name);
                    if (otherClass.isPrivate) {
                        TypeError("Other class private method called", stmt.posn);
                    }
                } else if (idDeclaration instanceof ClassDecl) {
                    if (table.findMethodInClass(idDeclaration.name, callMethod.name) instanceof MethodDecl) {
                        MethodDecl methodInOtherClass = (MethodDecl) table.findMethodInClass(idDeclaration.name,
                                callMethod.name);
                        if (!methodInOtherClass.isStatic) {
                            TypeError("Static method illegal in current context", methodInOtherClass.posn);
                        }
                        if (methodInOtherClass.isPrivate) {
                            TypeError("Private method illegal in current context", methodInOtherClass.posn);
                        }
                    } else {
                        TypeError("Member does not exist in class", callMethod.posn);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        if (stmt.returnExpr != null){
            stmt.returnExpr.visit(this, null);
        }
        return null;
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
       expr.functionRef.visit(this, arg);
       for (Expression expression : expr.argList){
           expression.visit(this, table);
       }
       return null;
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
        expr.classtype.visit(this, null);
        return null;
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
        ref.declaration = table.find(table.getCurrentClassName());
        if (table.getCurrentMethod().isStatic)
            TypeError("Use of \"this\" in a static method", ref.posn);
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        if (ref.id.spelling.equals(this.forbiddenVariable)){
            TypeError("Self-referencing in initialization", ref.posn);
        }
        ref.id.visit(this, arg);
        ref.declaration = ref.id.declaration;
        if (ref.declaration instanceof MethodDecl){
            MethodDecl methodDeclaration = (MethodDecl) ref.declaration;
            if (methodDeclaration.isStatic != table.getCurrentMethod().isStatic){
                TypeError("Reference does not match static context", ref.posn);
            }
        }
        else if (ref.declaration instanceof FieldDecl){
            FieldDecl fieldDeclaration = (FieldDecl) ref.declaration;
            if (fieldDeclaration.isStatic != table.getCurrentMethod().isStatic){
                TypeError ("Reference does not match static context", ref.posn);
            }
        }
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        ref.ref.visit(this, arg);
        String currentClassName = table.getCurrentClassName();
        if (ref.ref.declaration.type instanceof ClassType){
            String className = ((ClassType) ref.ref.declaration.type).className.spelling;
            table.changeClassView(className);
        }
        if (ref.ref.declaration.type.typeKind != TypeKind.ARRAY && !ref.id.spelling.equals("length")){
            ref.id.visit(this, table);
        }
        else{
            ref.id.declaration = new FieldDecl(false, false,
                    new BaseType(TypeKind.INT, ref.ref.posn), "length", ref.ref.posn);
        }
        ref.declaration = ref.id.declaration;
        table.changeClassView(currentClassName);
        if (ref.ref instanceof IdRef){
            if (ref.id.declaration instanceof FieldDecl){
                FieldDecl fieldDeclaration = (FieldDecl) ref.id.declaration;
                if (table.isStaticContext() && !fieldDeclaration.isStatic){
                    TypeError("Failure to meet static context", ref.posn);
                }
            }
        }
        if (ref.ref.declaration instanceof MethodDecl){
            TypeError("Function ref cannot appear within qualified ref", ref.posn);
        }
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        id.declaration = table.find(id.spelling);
        if (id.declaration == null){
            TypeError(id.spelling + " has not been declared", id.posn);
            System.exit(4);
        }
        String className = table.getCurrentClassName();
        if (id.declaration instanceof FieldDecl && table.findMethodInClass(className, id.spelling)
                == null){
            FieldDecl fieldDeclaration = (FieldDecl) id.declaration;
            if (fieldDeclaration.isPrivate){
                TypeError("Private field accessed in illegal context", fieldDeclaration.posn);
            }
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
