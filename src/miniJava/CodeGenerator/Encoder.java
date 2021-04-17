package miniJava.CodeGenerator;

import mJAM.*;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

import javax.sound.midi.Patch;
import javax.xml.transform.Source;
import java.lang.reflect.Method;
import java.util.ArrayList;
import static mJAM.Machine.Reg.*;
import mJAM.Machine.*;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.TokenKind;

import java.io.*;

public class Encoder implements Visitor<Integer, Object>{

    public int mainOffset;
    public final boolean debug = true;
    private ErrorReporter reporter;
    private int mainPatchAddress;
    private int frameCount = 0;
    private ArrayList<PatchDetail> patchList = new ArrayList<PatchDetail>();

    public Encoder(ErrorReporter reporter){
        this.reporter = reporter;
    }

    public final void beginEncode(AST ast){
        Machine.initCodeGen();
        ast.visit(this, Integer.MIN_VALUE);
        for (PatchDetail pd: patchList){
            Machine.patch(pd.address, ((KnownAddress)pd.declaration.runtimeEntity).address.displacement);
        }
    }

    @Override
    public Object visitPackage(Package prog, Integer arg) {
        Machine.emit(Op.LOADL, 0);
        int push = 0;

        for (ClassDecl c: prog.classDeclList){
            for (FieldDecl fd: c.fieldDeclList){
                if (fd.isStatic){
                    Machine.emit(Op.PUSH, 1);
                    fd.runtimeEntity = new KnownAddress(Machine.characterSize, ++push);
                }
            }
        }

        mainPatchAddress = Machine.nextInstrAddr();
        Machine.emit(Op.CALL, Reg.CB, -1);
        Machine.emit(Op.POP, 0, 0, push);
        Machine.emit(Op.HALT, 0, 0, 0);

        //Decorate
        for (ClassDecl c: prog.classDeclList){
            if (debug) System.out.println("Attempting to visit " + c);
            c.visit(this, 0);
        }

        //Visit
        for (ClassDecl c: prog.classDeclList){
            if (debug) System.out.println("Attempting to visit " + c + " - second round");
            c.visit(this, null);
        }
        return null;
    }

    public void codeGenError(String error, SourcePosition posn){
        System.out.println("*** line " + posn.start + "***CodeGenError: " + error);
        System.exit(4);
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Integer arg) {
        if (arg != null && arg == 0){
            for (int i = 0; i < cd.fieldDeclList.size(); i++){
                if (debug) System.out.println("Attempting to visit " + cd.fieldDeclList.get(i));
                cd.fieldDeclList.get(i).visit(this, i);
            }
        } else{
            for (MethodDecl md : cd.methodDeclList){
                if (debug) System.out.println("Attempting to visit " + md);
                md.visit(this, null);
            }
        }
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Integer arg) {
        if (!fd.isStatic){
            fd.runtimeEntity = new KnownAddress(Machine.characterSize, arg);
        }
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Integer arg) {
        md.runtimeEntity = new KnownAddress(Machine.characterSize, Machine.nextInstrAddr());

        if (md.name.equals("main")){
            Machine.patch(mainPatchAddress, Machine.nextInstrAddr());
        }

        int parameterOffset = -(md.parameterDeclList.size());
        for (ParameterDecl parameterDecl: md.parameterDeclList){
            if (debug) System.out.println("Attempting to visit " + parameterDecl);
            parameterDecl.visit(this, parameterOffset++);
        }

        frameCount = 3;
        for (Statement statement: md.statementList){
            if (debug) System.out.println("Attempting to visit " + statement);
            statement.visit(this, Integer.MIN_VALUE);
        }

        if (md.type.typeKind != TypeKind.VOID){
            if (debug) System.out.println("Attempting to visit " + md);
            md.type.visit(this, -1);
            if (!md.name.equals("println"))
                Machine.emit(Machine.Op.RETURN, 1, 0, md.parameterDeclList.size());
        } else{
            if (!md.name.equals("println"))
                Machine.emit(Machine.Op.RETURN, 0, 0, md.parameterDeclList.size());
        }
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Integer arg) {
        pd.runtimeEntity = new KnownAddress(Machine.addressSize, arg);
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Integer arg) {
        decl.runtimeEntity = new KnownAddress(Machine.characterSize, frameCount);
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Integer arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Integer arg) {
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Integer arg) {
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Integer arg) {
        int variableCount = 0;
        for (Statement statement : stmt.sl){
            if (statement instanceof VarDeclStmt){
                variableCount++;
            }
            if (debug) System.out.println("Attempting to visit " + statement);
            statement.visit(this, arg);
        }
        Machine.emit(Machine.Op.POP, 0, 0, variableCount);
        frameCount -= variableCount;
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
        if (debug) System.out.println("Attempting to visit " + stmt.varDecl);
        stmt.varDecl.visit(this, arg);
        if (debug) System.out.println("Attempting to visit " + stmt.initExp);
        stmt.initExp.visit(this, -1);
        frameCount++;
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Integer arg) {
        if (stmt.ref instanceof IdRef){
            int offset = (int) stmt.ref.visit(this, -2);
            if (debug) System.out.println("Attempting to visit " + stmt.val);
            stmt.val.visit(this, -1);
            if (stmt.ref.declaration instanceof FieldDecl && ((FieldDecl) (stmt.ref.declaration)).isStatic){
                Machine.emit(Machine.Op.STORE, SB, offset);
            } else if (stmt.ref.declaration instanceof FieldDecl){
                Machine.emit(Machine.Op.STORE, Machine.Reg.OB, offset);
            } else{
                Machine.emit(Machine.Op.STORE, Machine.Reg.LB, offset);
            }
        }
        else if (stmt.ref instanceof QualRef){
            if (stmt.ref.declaration instanceof FieldDecl && ((FieldDecl) (stmt.ref.declaration)).isStatic){
                int offset = (int) stmt.ref.visit(this, -2);
                if (debug) System.out.println("Attempting to visit " + stmt.val);
                stmt.val.visit(this, -1);
                Machine.emit(Machine.Op.STORE, SB, offset);
            } else{
                if (debug) System.out.println("Attempting to visit " + stmt.ref);
                stmt.ref.visit(this, -2);
                if (debug) System.out.println("Attempting to visit " + stmt.val);
                stmt.val.visit(this, -1);
                Machine.emit(Prim.fieldupd);
            }
        }
        else if (stmt.val instanceof IxExpr){ //TODO - Likely source of issues
            if (debug) System.out.println("Attempting to visit " + stmt.ref);
            stmt.ref.visit(this, -2);
            if (debug) System.out.println("Attempting to visit " + stmt.val);
            stmt.val.visit(this, -1);
            Machine.emit(Prim.arrayupd);
        }
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Integer arg) {
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Integer arg) {
        for (Expression expression : stmt.argList){
            if (debug) System.out.println("Attempting to visit " + expression);
            expression.visit(this, -1);
        }

        if (stmt.methodRef instanceof QualRef && stmt.methodRef.declaration instanceof MethodDecl){
            Machine.emit(Machine.Prim.putintnl);
        } else if (stmt.methodRef instanceof IdRef){
            if (((MethodDecl)stmt.methodRef.declaration).isStatic){
                patchList.add(new PatchDetail(Machine.nextInstrAddr(), stmt.methodRef.declaration));
                Machine.emit(Machine.Op.CALL, Machine.Reg.CB, -1);
            } else{
                Machine.emit(Machine.Op.LOAD, Machine.Reg.LB, 0);
                patchList.add(new PatchDetail(Machine.nextInstrAddr(), stmt.methodRef.declaration));
                Machine.emit(Machine.Op.CALLI, Machine.Reg.CB, -1);
            }
        }
        else{
            stmt.methodRef.visit(this, -1);
            patchList.add(new PatchDetail(Machine.nextInstrAddr(), stmt.methodRef.declaration));
            Machine.emit(Machine.Op.CALLI, Machine.Reg.CB, -1);
        }

        if (stmt.methodRef.declaration.type.typeKind != TypeKind.VOID){
            Machine.emit(Machine.Op.POP, 0, 0, 1);
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Integer arg) {
        if (stmt.returnExpr != null){
            if (debug) System.out.println("Attempting to visit " + stmt.returnExpr);
            stmt.returnExpr.visit(this, 1);
        }
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Integer arg) {
        if (debug) System.out.println("Attempting to visit " + stmt.cond);
        stmt.cond.visit(this, -1);

        int ifAddress = Machine.nextInstrAddr();
        Machine.emit(Machine.Op.JUMPIF, 0, Machine.Reg.CB, -1);

        if (debug) System.out.println("Attempting to visit " + stmt.thenStmt);
        stmt.thenStmt.visit(this, arg);
        int thenAddress = Machine.nextInstrAddr();
        Machine.emit(Op.JUMP, Machine.Reg.CB, -1);

        int elseAddress = Machine.nextInstrAddr();
        Machine.patch(ifAddress, elseAddress);
        if (stmt.elseStmt != null){
            if (debug) System.out.println("Attempting to visit " + stmt.elseStmt);
            stmt.elseStmt.visit(this, arg);
        }

        int lastAddress = Machine.nextInstrAddr();
        Machine.patch(thenAddress, lastAddress);
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Integer arg) {
        int whileAddress = Machine.nextInstrAddr();
        if (debug) System.out.println("Attempting to visit " + stmt.cond);
        stmt.cond.visit(this, -1);
        int afterAddress = Machine.nextInstrAddr();
        Machine.emit(Op.JUMPIF, 0, Reg.CB, -1);
        if (debug) System.out.println("Attempting to visit " + stmt.body);
        stmt.body.visit(this, arg);
        Machine.emit(Op.JUMP, Reg.CB, whileAddress);
        int lastAddress = Machine.nextInstrAddr();
        Machine.patch(afterAddress, lastAddress);
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Integer arg) {
        if (debug) System.out.println("Attempting to visit " + expr.expr);
        expr.expr.visit(this, -1);
        if (expr.operator != null){
            if (expr.operator.kind == TokenKind.EXCLAMATION){
                Machine.emit(Machine.Op.LOADL, 0);
                Machine.emit(Machine.Prim.eq);
            } else if (expr.operator.kind == TokenKind.MINUS){
                Machine.emit(Machine.Prim.neg);
            }
            else{
               codeGenError("Unrecognized unary character", expr.posn);
               System.exit(4);
            }
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Integer arg) {
        if (expr.operator.kind == TokenKind.AND || expr.operator.kind == TokenKind.OR){
            if (debug) System.out.println("Attempting to visit " + expr.left);
            expr.left.visit(this, -1);
            int comparisonAddress = Machine.nextInstrAddr();
            Machine.emit(Op.JUMPIF, (expr.operator.kind == TokenKind.AND) ? 0 : 1, Reg.CB, -1);
            Machine.emit(Op.LOADL, (expr.operator.kind == TokenKind.AND) ? 1 : 0);

            if (debug) System.out.println("Attempting to visit " + expr.right);
            expr.right.visit(this, 1);
            Machine.emit((expr.operator.kind == TokenKind.AND) ? Prim.and : Prim.or);
            int endAddress = Machine.nextInstrAddr();
            Machine.emit(Op.JUMP, Reg.CB, -1);

            Machine.patch(comparisonAddress, Machine.nextInstrAddr());
            Machine.emit(Op.LOADL, (expr.operator.kind == TokenKind.AND) ? 0 : 1);
            Machine.patch(endAddress, Machine.nextInstrAddr());
            return null;
        } else{
            if (debug) System.out.println("Attempting to visit " + expr.left);
            expr.left.visit(this, -1);
            if (debug) System.out.println("Attempting to visit " + expr.right);
            expr.right.visit(this, -1);
            switch(expr.operator.kind){
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
                case EQUALS:
                   Machine.emit(Prim.eq);
                   break;
                case GTHAN:
                    Machine.emit(Prim.gt);
                    break;
                case GEQUAL:
                    Machine.emit(Prim.ge);
                    break;
                case LTHAN:
                    Machine.emit(Prim.lt);
                    break;
                case LEQUAL:
                    Machine.emit(Prim.le);
                    break;
                case NEQUAL:
                    Machine.emit(Prim.ne);
                    break;
                default:
                    codeGenError("Binary operator not recognized", expr.posn);
                    break;
            }
        }
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Integer arg) {
        if (debug) System.out.println("Attempting to visit " + expr.ref);
        expr.ref.visit(this, arg);
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Integer arg) {
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Integer arg) {
        for (Expression expression : expr.argList){
            if (debug) System.out.println("Attempting to visit " + expression);
            expression.visit(this, -1);
        }

        if (expr.functionRef instanceof IdRef){
            if (((MethodDecl) expr.functionRef.declaration).isStatic){
                patchList.add(new PatchDetail(Machine.nextInstrAddr(), expr.functionRef.declaration));
                Machine.emit(Op.CALL, Reg.CB, -1);
            } else{
                Machine.emit(Op.LOADA, Reg.OB, 0);
                patchList.add(new PatchDetail(Machine.nextInstrAddr(), expr.functionRef.declaration));
                Machine.emit(Op.CALLI, Reg.CB, -1);
            }
        } else if (expr.functionRef.declaration.id.spelling.equals("println")){
            Machine.emit(Prim.putintnl);
        } else{
            if (debug) System.out.println("Attempting to visit " + expr.functionRef);
            expr.functionRef.visit(this, -1);
            patchList.add(new PatchDetail(Machine.nextInstrAddr(), expr.functionRef.declaration));
            Machine.emit(Op.CALLI, Machine.Reg.CB, -1);
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Integer arg) {
        int value = Integer.MIN_VALUE; //If you see min value, something is very, very wrong.
        switch(expr.lit.kind){
            case TRUE:
                value = 1;
                break;
            case FALSE:
                value = 0;
                break;
            case NUM:
                value = Integer.parseInt(expr.lit.spelling);
                break;
            case NULL:
                value = 0;
                break;
            default:
                codeGenError("Literal Expression unrecognized", expr.posn);
        }
        Machine.emit(Op.LOADL, value);
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Integer arg) {
        Machine.emit(Op.LOADL, -1);
        Machine.emit(Op.LOADL, expr.classtype.runtimeEntity.size);
        Machine.emit(Prim.newobj);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Integer arg) {
        if (debug) System.out.println("Attempting to visit " + expr.sizeExpr);
        expr.sizeExpr.visit(this, -1);
        Machine.emit(Prim.newarr);
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Integer arg) {
        Machine.emit(Op.LOADA, Reg.OB, 0);
        return null;
    }

    //-1 = fetch
    //-2 = store
    @Override
    public Object visitIdRef(IdRef ref, Integer arg) {
        ObjectAddress address = ((KnownAddress)ref.id.declaration.runtimeEntity).address;
        if (arg == -1){
            if (ref.id.declaration instanceof FieldDecl && ((FieldDecl)ref.id.declaration).isStatic){
                Machine.emit(Op.LOAD, Reg.SB, address.displacement);
            } else if (ref.id.declaration instanceof FieldDecl){
                Machine.emit(Op.LOAD, Reg.OB, address.displacement);
            } else{
                Machine.emit(Op.LOAD, Reg.LB, address.displacement);
            }
        } else if (arg == -2){
            return address.displacement;
        }
        else{
            codeGenError("Failed fetch store", ref.posn);
        }
        return null;
    }

    //-1 = fetch
    //-2 = store
    //Really should have just made these a global constant, but a bit late now.
    @Override
    public Object visitQRef(QualRef ref, Integer arg) {
        if (arg == -1){
            if (ref.id.declaration instanceof FieldDecl && ((FieldDecl)ref.id.declaration).isStatic){
                Machine.emit(Op.LOAD, Reg.SB, ((KnownAddress)ref.id.declaration.runtimeEntity).address.displacement);
            }
            else if (ref.id.declaration instanceof FieldDecl &&
                    ((FieldDecl)ref.id.declaration).id.spelling.equals("length")){
                if (debug) System.out.println("Attempting to visit " + ref.ref);
                ref.ref.visit(this, -1);
                Machine.emit(Op.LOADL, -1);
                Machine.emit(Prim.add);
                Machine.emit(Op.LOADI);
            }
            else if (ref.id.declaration instanceof FieldDecl){
                if (debug) System.out.println("Attempting to visit " + ref.ref);
                ref.ref.visit(this, -1);
                Machine.emit(Op.LOADL, ((KnownAddress) ref.id.declaration.runtimeEntity).address.displacement);
                Machine.emit(Prim.fieldref);
            } else if (ref.id.declaration instanceof MethodDecl){
                if (debug) System.out.println("Attempting to visit " + ref.ref);
                ref.ref.visit(this, -1);
            } else{
                codeGenError("Failed to fetch", ref.posn);
            }
        } else if (arg == -2){
            if (ref.id.declaration instanceof FieldDecl && ((FieldDecl) ref.id.declaration).isStatic){
                ObjectAddress address = ((KnownAddress) ref.id.declaration.runtimeEntity).address;
                return address.displacement;
            }
            else if (ref.id.declaration instanceof FieldDecl &&
                    ((FieldDecl)ref.id.declaration).id.spelling.equals("length")){
                codeGenError("Invalid attempted assignment", ref.posn);
            }
            else{
                if (debug) System.out.println("Attempting to visit " + ref.ref);
                ref.ref.visit(this, -1);
                ObjectAddress address = ((KnownAddress) ref.id.declaration.runtimeEntity).address;
                Machine.emit(Op.LOADL, address.displacement);
                return address.displacement;
            }
        } else{
            codeGenError("Unrecognized fetch/store", ref.posn);
        }
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Integer arg) {
        ObjectAddress address = ((KnownAddress) id.declaration.runtimeEntity).address;
        if (arg == 1){
            if (id.declaration instanceof FieldDecl && ((FieldDecl) id.declaration).isStatic){
                Machine.emit(Op.LOAD, Reg.SB, address.displacement);
            } else if (id.declaration instanceof FieldDecl){
                Machine.emit(Op.LOAD, Reg.OB, address.displacement);
            } else{
                Machine.emit(Op.LOAD, Reg.LB, address.displacement);
            }
        } else{
            return address.displacement;
        }
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Integer arg) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Integer arg) {
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Integer arg) {
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nullLiteral, Integer arg) {
        return null;
    }
}
