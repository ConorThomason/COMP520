package miniJava.CodeGenerator;

import mJAM.*;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

import java.util.ArrayList;
import mJAM.Machine.*;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.TokenKind;

import java.util.Stack;

public class Encoder implements Visitor<Integer, Object>{
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public int staticAddress;
    public int localDisplacement;
    public int localPopCount;
    public int methodParams;
    public final boolean debug = true;
    private ErrorReporter reporter;
    private int mainPatchAddress;
    private ArrayList<PatchDetail> patchList = new ArrayList<PatchDetail>();

    public Encoder(ErrorReporter reporter){
        this.reporter = reporter;
    }

    public final void beginEncode(AST ast, String fileName){
        Machine.initCodeGen();
        staticAddress = Machine.nextInstrAddr();
        Machine.emit(Op.PUSH, 0);
        Machine.emit(Op.LOADL, 0);
        Machine.emit(Prim.newarr);

        mainPatchAddress = Machine.nextInstrAddr();
        Machine.emit(Op.CALL, Reg.CB, 0);
        Machine.emit(Op.HALT, 0, 0, 0);
        ast.visit(this, Integer.MIN_VALUE);
    }

    @Override
    public Object visitPackage(Package prog, Integer arg) {
        int staticOffset = 0;
        for (ClassDecl cd: prog.classDeclList){
            int instanceOffset = 0;
            for (FieldDecl fd: cd.fieldDeclList){
                if (fd.isStatic){
                    fd.runtimeEntity = new RuntimeEntity(staticOffset++);
                } else{
                    fd.runtimeEntity = new RuntimeEntity(instanceOffset++);
                }
            }
            cd.runtimeEntity = new RuntimeEntity(instanceOffset);
        }
        Machine.patch(staticAddress, staticOffset);
        Machine.patch(mainPatchAddress, Machine.nextInstrAddr());
        int mainMethods = 0;
        for (ClassDecl c: prog.classDeclList){
            for (MethodDecl m: c.methodDeclList) {
                if (m.name.equals("main")) {
                    mainMethods++;
                    if (m.isPrivate || !m.isStatic || m.type.typeKind != TypeKind.VOID) {
                        codeGenError("Malformed main", m.posn);
                    }
                    if (m.parameterDeclList.size() == 1) {
                        if (m.parameterDeclList.get(0).type instanceof ArrayType) {
                            if (((ArrayType) m.parameterDeclList.get(0).type).eltType instanceof ClassType &&
                                    ((ClassType) ((ArrayType) m.parameterDeclList.get(0).type).eltType)
                                            .className.spelling.equals("String")) {
                                patchList.add(new PatchDetail(mainPatchAddress, m));
                            } else {
                                codeGenError("Malformed main", m.posn);
                            }
                        } else {
                            codeGenError("Malformed main", m.posn);
                        }
                    } else {
                        codeGenError("Malformed main", m.posn);
                    }
                }
            }
        }
        if (mainMethods != 1){
            codeGenError("Failed to use unique main(String[] args) method", prog.posn);
        }
        for (ClassDecl c: prog.classDeclList){
            for (MethodDecl md: c.methodDeclList){
                StatementList list = md.statementList;
                if (list.size() == 0){
                    md.statementList.add(new ReturnStmt(null, md.posn));
                }
                Statement finalStmt = list.get(list.size()-1);
                if (md.type.typeKind != TypeKind.VOID) {
                    if (!(finalStmt instanceof ReturnStmt)) {
                        codeGenError("Non-void method doesn't return", finalStmt.posn);
                    }
                } else {
                    md.statementList.add(new ReturnStmt(null, finalStmt.posn));
                }
            }
        }
        //TODO - may need main check
        for (ClassDecl c: prog.classDeclList){
            if (debug) System.out.println("Attempting to visit " + c);
            c.visit(this, Integer.MIN_VALUE);
        }

        for (PatchDetail pd: patchList){
            Machine.patch(pd.address, pd.declaration.runtimeEntity.memoryOffset);
        }
        return null;
    }

    public void codeGenError(String error, SourcePosition posn){
        System.out.println("*** line " + posn.start + "***CodeGenError: " + error);
        System.exit(4);
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Integer arg) {
        for (FieldDecl f: cd.fieldDeclList){
            if (debug) System.out.println("Attempting to visit " + f);
            f.visit(this, Integer.MIN_VALUE);
        }
        for (MethodDecl m: cd.methodDeclList){
            if (debug) System.out.println("Attempting to visit " + m);
            m.visit(this, Integer.MIN_VALUE);
        }
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Integer arg) {
        if (debug) System.out.println("Attempting to visit " + fd);
        fd.type.visit(this, Integer.MIN_VALUE);
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Integer arg) {
        localDisplacement = 3;
        methodParams = md.parameterDeclList.size();
        if (debug) System.out.println("Attempting to visit " + md.type);
        md.type.visit(this, Integer.MIN_VALUE);
        int paramOffset = -md.parameterDeclList.size();
        for (ParameterDecl pd: md.parameterDeclList){
            if (debug) System.out.println("Attempting to visit " + pd);
            pd.visit(this, Integer.MIN_VALUE);
            pd.runtimeEntity = new RuntimeEntity(paramOffset++);
        }

        md.runtimeEntity = new RuntimeEntity(Machine.nextInstrAddr());
        for (Statement s: md.statementList){
            if (debug) System.out.println("Attempting to visit " + ANSI_RED + "Statement: " + s + ANSI_RESET);
            s.visit(this, Integer.MIN_VALUE);
        }
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Integer arg) {
        pd.type.visit(this, Integer.MIN_VALUE);
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Integer arg) {
        decl.runtimeEntity = new RuntimeEntity(localDisplacement++);
        decl.type.visit(this, Integer.MIN_VALUE);
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Integer arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Integer arg) {
        type.className.visit(this, Integer.MIN_VALUE);
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Integer arg) {
        type.eltType.visit(this, Integer.MIN_VALUE);
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Integer arg) {
        localPopCount = 0;
        for (Statement s: stmt.sl){
            s.visit(this, Integer.MIN_VALUE);
        }
        if (localPopCount > 0){
            localDisplacement = localDisplacement - localPopCount;
            Machine.emit(Op.POP, localPopCount);
        }
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
        localPopCount++;
        stmt.varDecl.visit(this, Integer.MIN_VALUE);
        stmt.initExp.visit(this, Integer.MIN_VALUE);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Integer arg) {
        if (stmt.ref.declaration instanceof MemberDecl && ((MemberDecl)stmt.ref.declaration).isStatic){
            if (debug) System.out.println("Attempting to visit " + stmt.val);
            stmt.val.visit(this, Integer.MIN_VALUE);
            Machine.emit(Op.STORE, Reg.SB,
                    stmt.ref.declaration.runtimeEntity.memoryOffset);
        } else if (stmt.ref instanceof IdRef){
            IdRef reference = (IdRef) stmt.ref;
            if (reference.declaration instanceof FieldDecl){
                Machine.emit(Op.LOADA, Reg.OB, 0);
                Machine.emit(Op.LOADL, reference.id.declaration.runtimeEntity.memoryOffset);
                if (debug) System.out.println("Attempting to visit " + stmt.val);
                stmt.val.visit(this, Integer.MIN_VALUE);
                Machine.emit(Prim.fieldupd);
            } else{
                if (debug) System.out.println("Attempting to visit " + stmt.val);
                stmt.val.visit(this, Integer.MIN_VALUE);
                storeId((IdRef) stmt.ref);
            }
        } else if (stmt.ref instanceof QualRef){
            qRefUtil((QualRef) stmt.ref);
            stmt.val.visit(this, Integer.MIN_VALUE);
            Machine.emit(Prim.fieldupd);
        }
        return null;
    }

    public void storeId(IdRef ref){
        if (ref.declaration instanceof FieldDecl){
            FieldDecl declaration = (FieldDecl) ref.declaration;
            if (declaration.isStatic){
                Machine.emit(Op.STORE, Reg.SB, ref.id.declaration.runtimeEntity.memoryOffset);
            }
        }
        else{
            if (ref.id.declaration instanceof MemberDecl && ((MemberDecl)ref.id.declaration).isStatic){
                Machine.emit(Op.STORE, Reg.SB, ref.id.declaration.runtimeEntity.memoryOffset);
            } else{
                Machine.emit(Op.STORE, Reg.LB, ref.id.declaration.runtimeEntity.memoryOffset);
            }
        }
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Integer arg) {
        for (Expression e: stmt.argList){
            e.visit(this, Integer.MIN_VALUE);
        }

        if (((MethodDecl)stmt.methodRef.declaration).name.equals("println") && ((MethodDecl)stmt.methodRef.declaration).isPredefined){
            if (stmt.argList.size() == 1){
                Machine.emit(Prim.putintnl);
            } else{
                codeGenError("More arguments into println than allowed", stmt.posn);
            }
        } else{
            int callAddress = Machine.nextInstrAddr();
            if (((MethodDecl) stmt.methodRef.declaration).isStatic){
                Machine.emit(Op.CALL, Reg.CB, 0);
                patchList.add(new PatchDetail(callAddress,
                        (MethodDecl) stmt.methodRef.declaration));
            }
            else {
                stmt.methodRef.visit(this, Integer.MIN_VALUE);
                if (stmt.methodRef instanceof QualRef) {
                    QualRef qRef = (QualRef) stmt.methodRef;
                    Reference methodRef = qRef.ref;
                    methodRef.visit(this, null);
                } else {
                    visitThisRef(null, null);
                }
                callAddress = Machine.nextInstrAddr();
                Machine.emit(Op.CALLI, Reg.CB, 0);
                patchList.add(new PatchDetail(callAddress,
                        (MethodDecl) stmt.methodRef.declaration));
            }
        }
        if (stmt.methodRef.declaration.type.typeKind != TypeKind.VOID){
            Machine.emit(Op.POP, 1);
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Integer arg) {
        if (stmt.returnExpr != null){
            stmt.returnExpr.visit(this, Integer.MIN_VALUE);
        }
        if (stmt.returnExpr == null){
            Machine.emit(Op.RETURN, 0, 0, methodParams);
        } else{
            Machine.emit(Op.RETURN, 1, 0, methodParams);
        }
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Integer arg) {
        stmt.cond.visit(this, Integer.MIN_VALUE);
        int firstJump = Machine.nextInstrAddr();
        Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);
        stmt.thenStmt.visit(this, Integer.MIN_VALUE);
        int secondJump = Machine.nextInstrAddr();
        Machine.emit(Op.JUMP, 0, Reg.CB, 0);
        Machine.patch(firstJump, Machine.nextInstrAddr());
        if (stmt.elseStmt != null)
            stmt.elseStmt.visit(this, Integer.MIN_VALUE);

        Machine.patch(secondJump, Machine.nextInstrAddr());
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Integer arg) {
        int firstJump = Machine.nextInstrAddr();
        Machine.emit(Op.JUMP, 0, Reg.CB, 0);
        stmt.body.visit(this, Integer.MIN_VALUE);
        int secondJump = Machine.nextInstrAddr();
        stmt.cond.visit(this, Integer.MIN_VALUE);
        Machine.emit(Op.JUMPIF, 1, Reg.CB, firstJump + 1);
        Machine.patch(firstJump, secondJump);
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Integer arg) {
        if (expr.operator.kind == TokenKind.MINUS){
            Machine.emit(Op.LOADL, 0);
        }
        expr.expr.visit(this, Integer.MIN_VALUE);
        expr.operator.visit(this, Integer.MIN_VALUE);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Integer arg) {
        if (expr.operator.kind == TokenKind.AND || expr.operator.kind == TokenKind.OR){

            if (debug) System.out.println("Attempting to visit " + expr.left);
            expr.left.visit(this, Integer.MIN_VALUE);

            int comparisonAddress = Machine.nextInstrAddr();
            Machine.emit(Op.JUMPIF, (expr.operator.kind == TokenKind.AND) ? 0 : 1, Reg.CB, -1);
            Machine.emit(Op.LOADL, (expr.operator.kind == TokenKind.AND) ? 1 : 0);

            if (debug) System.out.println("Attempting to visit " + expr.right);
            expr.right.visit(this, Integer.MIN_VALUE);
            Machine.emit((expr.operator.kind == TokenKind.AND) ? Prim.and : Prim.or);
            int endAddress = Machine.nextInstrAddr();
            Machine.emit(Op.JUMP, Reg.CB, -1);

            Machine.patch(comparisonAddress, Machine.nextInstrAddr());
            Machine.emit(Op.LOADL, (expr.operator.kind == TokenKind.AND) ? 0 : 1);
            Machine.patch(endAddress, Machine.nextInstrAddr());
            return null;
        } else{
            if (debug) System.out.println("Attempting to visit " + expr.left);
            expr.left.visit(this, Integer.MIN_VALUE);
            if (debug) System.out.println("Attempting to visit " + expr.right);
            expr.right.visit(this, Integer.MIN_VALUE);
            if (debug) System.out.println("Attempting to visit " + expr.operator);
            expr.operator.visit(this, Integer.MIN_VALUE);
        }
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Integer arg) {
        if (debug) System.out.println("Attempting to visit " + expr.ref);
        if (expr.ref.declaration instanceof FieldDecl &&
                ((FieldDecl) expr.ref.declaration).isStatic) {
            Machine.emit(Op.LOAD, Reg.SB,
                    expr.ref.declaration.runtimeEntity.memoryOffset);
        } else if (expr.ref instanceof QualRef || expr.ref instanceof IdRef) {
            if (debug) System.out.println("Attempting to visit " + expr.ref);
            expr.ref.visit(this, Integer.MIN_VALUE);
        } else if (expr.ref instanceof ThisRef) {
            Machine.emit(Op.LOADA, Reg.OB, 0);
        }

            //TODO - Ix needed?
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Integer arg) {
        expr.ref.visit(this, Integer.MIN_VALUE);
        expr.ixExpr.visit(this, Integer.MIN_VALUE);
        Machine.emit(Prim.arrayref);
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Integer arg) {
        if (debug) System.out.println("Attempting to visit " + stmt.ref + " - IxAssignStmt");
        stmt.ref.visit(this, Integer.MIN_VALUE);
        if (debug) System.out.println("Attempting to visit " + stmt.ix+ " - IxAssignStmt");
        stmt.ix.visit(this, Integer.MIN_VALUE);
        if (debug) System.out.println("Attempting to visit " + stmt.exp + " - IxAssignStmt");
        stmt.exp.visit(this, Integer.MIN_VALUE);
        Machine.emit(Prim.arrayupd);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Integer arg) {
        for (Expression expression : expr.argList){
            if (debug) System.out.println("Attempting to visit " + expression);
            expression.visit(this, Integer.MIN_VALUE);
        }

        if (expr.functionRef.declaration != null && !expr.functionRef.declaration.name.equals("println")){
            expr.functionRef.visit(this, Integer.MIN_VALUE);
            int callAddress = Machine.nextInstrAddr();
            if (((MethodDecl) expr.functionRef.declaration).isStatic){
                Machine.emit(Op.CALL, Reg.CB, 0);
            } else{
                if (expr.functionRef instanceof QualRef){
                    QualRef ref = (QualRef) expr.functionRef;
                    if (debug) System.out.println("Attempting to visit " + ref.ref);
                    ref.ref.visit(this, Integer.MIN_VALUE);
                } else{
                    Machine.emit(Op.LOADA, Reg.OB, 0);
                }
                callAddress = Machine.nextInstrAddr();
                Machine.emit(Op.CALLI, Reg.CB, 0);
            }
            patchList.add(new PatchDetail(callAddress, (MethodDecl)expr.functionRef.declaration));
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Integer arg) {
        if (debug) System.out.println("Attempting to visit " + expr.lit);
        expr.lit.visit(this, Integer.MIN_VALUE);
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Integer arg) {
        Machine.emit(Op.LOADL, -1);
        Machine.emit(Op.LOADL, expr.classtype.className.declaration.runtimeEntity.memoryOffset);
        Machine.emit(Prim.newobj);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Integer arg) {
        if (debug) System.out.println("Attempting to visit " + expr.sizeExpr);
        expr.sizeExpr.visit(this, Integer.MIN_VALUE);
        Machine.emit(Prim.newarr);
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Integer arg) {
        Machine.emit(Op.LOADA, Reg.OB, 0);
        return null;
    }

    public void idRefShortcut(IdRef ref){
        if (ref.declaration instanceof FieldDecl && ref.id.declaration.runtimeEntity != null){
            FieldDecl declaration = (FieldDecl) ref.declaration;
            if (declaration.isStatic){
                Machine.emit(Op.LOAD, Reg.SB, ref.id.declaration.runtimeEntity.memoryOffset);
            } else{
                Machine.emit(Op.LOAD, Reg.OB, ref.id.declaration.runtimeEntity.memoryOffset);
            }
        } else if (ref.id.declaration != null){
            if (ref.id.declaration instanceof MemberDecl && ((MemberDecl)ref.id.declaration).isStatic){
                Machine.emit(Op.LOAD, Reg.SB, ref.id.declaration.runtimeEntity.memoryOffset);
            } else if (!(ref.id.declaration instanceof MethodDecl)){
                Machine.emit(Op.LOAD, Reg.LB, ref.id.declaration.runtimeEntity.memoryOffset);
            }
        }
    }

    @Override
    public Object visitIdRef(IdRef ref, Integer arg) {
        idRefShortcut(ref);
        return null;
    }

    public void qRefUtil(QualRef ref){
        if (ref.id.declaration != null && ref.id.declaration.runtimeEntity != null){
            Stack<Integer> offsetStack = new Stack<>();
            offsetStack.push(ref.id.declaration.runtimeEntity.memoryOffset);
            while (ref.ref instanceof QualRef){
                ref = (QualRef) ref.ref;
                offsetStack.push(ref.declaration.runtimeEntity.memoryOffset);
            }
            ref.ref.visit(this, Integer.MIN_VALUE);
            int size = offsetStack.size();
            for (int i = 0; i < size; i++){
                int offset = offsetStack.pop();
                Machine.emit(Op.LOADL, offset);
                if (i+1 < size){
                    Machine.emit(Prim.fieldref);
                }
            }
        }
    }

    @Override
    public Object visitQRef(QualRef ref, Integer arg) {
        if ((ref.id.declaration != null && ref.id.declaration.name.equals("length")) || ref.id.spelling.equals("length") ){
            if (ref.ref instanceof IdRef) {
                idRefShortcut((IdRef) ref.ref);
                Machine.emit(Prim.arraylen);
            }
        } else if (ref.id.declaration != null && ref.id.declaration.runtimeEntity != null){
            qRefUtil(ref);
            Machine.emit(Prim.fieldref);
        }
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Integer arg) {
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Integer arg) {
        switch (op.kind){
            case PLUS:
                Machine.emit(Prim.add);
                break;
            case MINUS:
                Machine.emit(Prim.sub);
                break;
            case TIMES:
                Machine.emit(Prim.mult);
                break;
            case DIVIDE:
                Machine.emit(Prim.div);
                break;
            case EQUIVALENT:
                Machine.emit(Prim.eq);
                break;
            case LEQUAL:
                Machine.emit(Prim.le);
                break;
            case LTHAN:
                Machine.emit(Prim.lt);
                break;
            case GEQUAL:
                Machine.emit(Prim.ge);
                break;
            case GTHAN:
                Machine.emit(Prim.gt);
                break;
            case AND:
                Machine.emit(Prim.and);
                break;
            case OR:
                Machine.emit(Prim.or);
                break;
            case EXCLAMATION:
                Machine.emit(Prim.neg);
                break;
            default:
                codeGenError("Unrecognized operator", op.posn);
                break;
        }
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Integer arg) {
        int providedInt = Integer.parseInt(num.spelling);
        Machine.emit(Op.LOADL, providedInt);
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Integer arg) {
        Machine.emit(Op.LOADL, (bool.spelling.equals("true")) ? Machine.trueRep: Machine.falseRep);
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nullLiteral, Integer arg) {
        Machine.emit(Op.LOADL, Machine.nullRep);
        return null;
    }
}
