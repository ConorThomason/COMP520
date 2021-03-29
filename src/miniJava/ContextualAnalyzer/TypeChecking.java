package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenKind;


public class TypeChecking implements Visitor<Object, Object> {
   final static boolean debug = true;

    public IdentificationTable table;
    private ErrorReporter reporter;
    private ClassType workingClass;
    private String forbiddenVariable;

    public BaseType errorType = new BaseType(TypeKind.ERROR, null);
    public BaseType unsupportedType = new BaseType(TypeKind.UNSUPPORTED, null);
    public BaseType nullType = new BaseType(TypeKind.NULL, null);

    public TypeChecking(Package ast, ErrorReporter reporter){
        this.reporter = reporter;
        table = new IdentificationTable(reporter);

        if (debug) System.out.println("Entering visitPackage");
        visitPackage(ast, table);
    }

    private TypeDenoter getType(TypeDenoter type){
        if (type == null) {
            if (debug) System.out.println("Null Type");
            return new BaseType(TypeKind.NULL, new SourcePosition());
        }
        return type;
    }
    private boolean typesEqual(TypeDenoter type1, TypeDenoter type2){
        if (type1 == null || type2 == null){
            return false;
        }
        if (errorUnsupportedCheck(type1, type2)){
            return false;
        }
        else if (type1 instanceof ArrayType || type2 instanceof ArrayType) {
            if (type1.typeKind == TypeKind.NULL || type2.typeKind == TypeKind.NULL)
                return true;
            if (!(type1 instanceof ArrayType) || !(type2 instanceof ArrayType)) {
                return false;
            }
            return typesEqual(((ArrayType) type1).eltType, ((ArrayType) type2).eltType);
        }
        else if (type1 instanceof ClassType || type2 instanceof ClassType){
            if (type1.typeKind == TypeKind.CLASS && type2.typeKind == TypeKind.NULL){
                return true;
            } else if (type1.typeKind == TypeKind.NULL || type2.typeKind == TypeKind.CLASS){
                return true;
            } else if (!(type1 instanceof ClassType) || !(type2 instanceof ClassType)){
                return false;
            }
            Identifier className1 = ((ClassType) type1).className;
            Identifier className2 = ((ClassType) type2).className;
            if (className1.declaration != null && className2.declaration != null){
                if (className1.declaration.type.typeKind == TypeKind.UNSUPPORTED || className2.declaration.type
                .typeKind == TypeKind.UNSUPPORTED){
                    return false;
                }
            }
            return className1.spelling.equals(className2.spelling);
        }
        return type1.typeKind == type2.typeKind;
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
            if (table.findClass(cd.name) != null){
                TypeError("Class already declared", cd.posn);
                System.exit(4);
            }
            else{
                table.insertClass(cd.name, cd);
            }
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
            return new BaseType(TypeKind.UNSUPPORTED, prog.posn);
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        //Probably not needed anymore since implementation continuity, but removing it seems like a needless risk?
        workingClass = new ClassType(new Identifier(new Token(TokenKind.ID, cd.name, null)), null);
        table.openScope();
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
        return getType(cd.type);
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        table.insert(fd.name, fd);
        fd.type.visit(this, arg);
        return getType(fd.type);
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {

        TypeDenoter toReturn = getType(md.type);

        if (md.type instanceof ClassType){
            if(table.findClass(((ClassType)md.type).className.spelling) == null){
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
            TypeDenoter typeDenoter = (TypeDenoter) s.visit(this, arg);
            if (s instanceof ReturnStmt && !typesEqual(typeDenoter, toReturn)){
                TypeError("Return statement has type that doesn't match function return type", s.posn);
            }
        }

        table.closeScope();
        table.closeScope();
        return toReturn;
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
        return getType(pd.type);
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        if (debug) System.out.println("Attempting to visit " + decl);
        table.insert(decl.name, decl);
        decl.type.visit(this, arg);
        return getType(decl.type);
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return getType(type);
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        type.className.visit(this, table);
        return getType(type);
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, table);
        return getType(type);
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        table.openScope();
        for (Statement s: stmt.sl){
            if (debug) System.out.println("Attempting to visit " + stmt);
            s.visit(this, "");
        }
        table.closeScope();
        return new BaseType(TypeKind.UNSUPPORTED, stmt.posn);
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
        TypeDenoter expressionType = (TypeDenoter) stmt.initExp.visit(this, null);
        this.forbiddenVariable = null;
        TypeDenoter referenceType = (TypeDenoter) stmt.varDecl.visit(this, null);


        if (stmt.initExp instanceof RefExpr){
            RefExpr referenceExpression = (RefExpr) stmt.initExp;
            if (referenceExpression.ref.declaration instanceof ClassDecl){
                TypeError("Variable declaration attempts assignment of class", stmt.posn);
            }
            if (referenceExpression.ref.declaration instanceof MethodDecl){
                TypeError("Variable declaration attempts assignment of method", stmt.posn);
            }
        }
        if (!typesEqual(referenceType, expressionType) || !referenceType.equals(expressionType)){
            if (referenceType instanceof ClassType && expressionType instanceof ClassType ){
                Identifier i1 = ((ClassType) referenceType).className;
                Identifier i2 = ((ClassType) expressionType).className;
                if (i1.declaration == null || i2.declaration == null){
                    TypeError("Variable declaration attempts assignment of class type", stmt.posn);
                }
                else{
                    TypeError("Variable declaration attempts to assign incompatible expression to reference",
                            stmt.posn);
                }
            }
            else{
                TypeError("Variable declaration attempts to assign incompatible expression to reference",
                        stmt.posn);
            }
            return new BaseType(TypeKind.ERROR, stmt.posn);
        }
        return referenceType;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        TypeDenoter referenceType = (TypeDenoter) stmt.ref.visit(this, null);
        TypeDenoter expressionType = (TypeDenoter) stmt.val.visit(this, null);
        if (stmt.val instanceof RefExpr){
            RefExpr referenceExpression = (RefExpr) stmt.val;
            if (referenceExpression.ref.declaration instanceof MethodDecl){
                TypeError("Attempted invalid assignment of method", stmt.posn);
            }
//            if (referenceExpression.ref instanceof IdRef){
//                String name = ((IdRef) referenceExpression.ref).id.spelling;
//                int level = table.getCurrentNode().getNodeLevel();
//                if (level == 0){
//                    TypeError("Illegal attempt to assign to class", stmt.posn);
//                }
//            }
        }
        if (stmt.ref instanceof QualRef){
            QualRef qualifiedReference = (QualRef) stmt.ref;
            if (qualifiedReference.ref.declaration instanceof ClassDecl){
                return new BaseType(TypeKind.INT, stmt.posn);
            }
            if (qualifiedReference.ref.declaration.type.typeKind == TypeKind.ARRAY &&
                    qualifiedReference.id.spelling.equals("length")){
                TypeError("Array length in assignment is illegal", stmt.posn);
                return new BaseType(TypeKind.ERROR, stmt.posn);

            }
        }
        if (!(referenceType.typeKind == expressionType.typeKind)){
            TypeError("Attempt to assign incompatible expression to reference", stmt.posn);
            return new BaseType(TypeKind.ERROR, stmt.posn);
        }
        return referenceType;
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
        if (!(stmt.methodRef.declaration instanceof MethodDecl)){
            TypeError("Illegal call to non-function", stmt.posn);
            return new BaseType(TypeKind.ERROR, stmt.posn);
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
        ParameterDeclList parameters = ((MethodDecl) stmt.methodRef.declaration).parameterDeclList;
        TypeDenoter returnType = (TypeDenoter) stmt.methodRef.visit(this, null);
        ExprList parameterList = stmt.argList;
        if (parameters.size() != parameterList.size()){
            TypeError("Function call has incorrect number of parameters", stmt.posn);
            return new BaseType(TypeKind.ERROR, stmt.posn);
        }
        for (int i = 0; i < parameterList.size(); i++){
            TypeDenoter parameterType = (TypeDenoter) parameters.get(i).type;
            TypeDenoter listType = (TypeDenoter) parameterList.get(i).visit(this, null);
            if (!typesEqual(parameterType, listType)){
                TypeError("Function call has parameter of incorrect type", stmt.posn);
            }
            return new BaseType(TypeKind.ERROR, stmt.posn);
        }
        return returnType;
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
        if (stmt.cond.visit(this, null) == TypeKind.BOOLEAN){
            TypeError("Block condition is not of type BOOLEAN", stmt.posn);
            return new BaseType(TypeKind.ERROR, stmt.posn);
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

        return new BaseType(TypeKind.UNSUPPORTED, stmt.posn);
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
        return new BaseType(TypeKind.UNSUPPORTED, stmt.posn);
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
        return expr.ref.visit(this, table);
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.ref.visit(this, arg);
        expr.ixExpr.visit(this, arg);
        return new BaseType(TypeKind.ARRAY, expr.posn);
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        if (!(expr.functionRef.declaration instanceof MethodDecl)){
            TypeError("Function call references function that doesn't exist", expr.posn);
            System.exit(4);
        }

        MethodDecl methodDeclaration = (MethodDecl) expr.functionRef.declaration;
        ParameterDeclList listParameters = methodDeclaration.parameterDeclList;
        TypeDenoter returnType = (TypeDenoter) expr.functionRef.visit(this, null);
        ExprList expressionParams = expr.argList;

        if (listParameters.size() != expressionParams.size()){
            TypeError("Function call has incorrect number of parameters", expr.posn);
            return new BaseType(TypeKind.ERROR, expr.posn);
        }
        for (int i = 0; i < listParameters.size(); i++){
            TypeDenoter listType = (TypeDenoter) listParameters.get(i).type;
            TypeDenoter expressionType = (TypeDenoter) expressionParams.get(i).visit(this, null);
            if (!typesEqual(listType, expressionType)){
                TypeError("Function call has incorrect parameter type", expr.posn);
                return new BaseType(TypeKind.ERROR, expr.posn);
            }
        }
       return returnType;
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
        return literalType;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        return expr.classtype.visit(this, null);
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        TypeDenoter returnedType = null;
        if (debug) System.out.println("Attempting to visit " + expr);
        TypeDenoter sizeType = (TypeDenoter) expr.sizeExpr.visit(this, arg);
        if (errorUnsupportedCheck(sizeType)){
            returnedType = errorType;
        }
        else{
            TypeDenoter arrayType = expr.eltType;
            if (!sizeType.equals(new BaseType(TypeKind.INT, null))){
                returnedType = errorType;
                TypeError("Array requires an int size", sizeType.posn);
            }
        }
        return getType(new ArrayType(expr.eltType, expr.posn));
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        ref.declaration = table.find(table.getCurrentClassName());
        if (table.getCurrentMethod() != null && table.getCurrentMethod().isStatic) {
            TypeError("Use of \"this\" in a static method", ref.posn);
            System.exit(4);
        }
        if (ref.declaration == null){
            return new BaseType(TypeKind.NULL, ref.posn);
        }
        return getType(ref.declaration.type);
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
            if (table.getCurrentMethod() != null && fieldDeclaration.isStatic != table.getCurrentMethod().isStatic){
                TypeError ("Reference does not match static context", ref.posn);
            }
        }
        return getType(ref.declaration.type);
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        TypeDenoter referenceType = (TypeDenoter) ref.ref.visit(this,null);
        String previousClassName = table.getCurrentClassName();
        if (ref.ref.declaration != null && ref.ref.declaration.type instanceof ClassType){
            String className = ((ClassType) ref.ref.declaration.type).className.spelling;
            table.changeClassView(className);
        }
        if (!(referenceType instanceof ClassType)){
            if (!(referenceType.typeKind == TypeKind.ARRAY) && ref.id.spelling.equals("length")){
                ref.id.visit(this, table);
            } else{
                ref.id.declaration = new FieldDecl(false, false, new BaseType(TypeKind.INT, ref.ref.posn),
                "length", ref.ref.posn);
            }
            ref.declaration = ref.id.declaration;
        }
        table.find(previousClassName);
        if (ref.ref instanceof IdRef){
            String name = ((IdRef) ref.ref).id.spelling;
            int level = table.getCurrentNode().getNodeLevel();
            if (ref.id.declaration instanceof FieldDecl){
                FieldDecl fieldDeclaration = (FieldDecl) ref.id.declaration;
                if (table.getCurrentMethod() != null && table.getCurrentMethod().isStatic && !fieldDeclaration.isStatic){
                    TypeError("Reference does not fit current static context", ref.posn);
                }
            }
        }
        return ref.id.visit(this, null);
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        id.declaration = table.findLocal(id.spelling);
        if (id.declaration == null){
            id.declaration = table.find(id.spelling);
            if (id.declaration == null) {
                TypeError(id.spelling + " has not been declared", id.posn);
                System.exit(4);
            }
        }
        String className = table.getCurrentClassName();
        if (id.declaration instanceof FieldDecl && table.findMethodInClass(className, id.spelling)
                == null){
            FieldDecl fieldDeclaration = (FieldDecl) id.declaration;
            if (fieldDeclaration.isPrivate){
                TypeError("Private field accessed in illegal context", fieldDeclaration.posn);
            }
        }
        return getType(id.declaration.type);
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