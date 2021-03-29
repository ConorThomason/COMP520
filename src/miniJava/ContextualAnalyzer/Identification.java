package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

public class Identification implements Visitor<Object, Object> {

    public ErrorReporter reporter;
    public Identification(ErrorReporter reporter){
        this.reporter = reporter;
    }

    @Override
    public Object visitPackage(Package prog, Object arg) {
        for (ClassDecl c : prog.classDeclList)
            c.visit(this, null);
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        for (FieldDecl f : cd.fieldDeclList)
            f.visit(this, null);
        for (MethodDecl m : cd.methodDeclList)
            m.visit(this, null);
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        fd.type.visit(this, null);
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        md.type.visit(this, null);
        for (ParameterDecl p : md.parameterDeclList)
            p.visit(this,null);
        for (Statement s: md.statementList)
            s.visit(this,null);
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        pd.type.visit(this, null);
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        decl.type.visit(this, null);
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        type.className.visit(this, null);
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, null);
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        for (Statement s: stmt.sl)
            s.visit(this, null);
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        stmt.initExp.visit(this, null);
        stmt.varDecl.visit(this, null);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.ref.visit(this, null);
        stmt.val.visit(this, null);
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ix.visit(this, arg);
        stmt.ref.visit(this, null);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        for (Expression e: stmt.argList)
            e.visit(this, null);
            stmt.methodRef.visit(this, null);
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        stmt.returnExpr.visit(this, null);
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        stmt.thenStmt.visit(this, null);
        if (stmt.elseStmt != null)
            stmt.elseStmt.visit(this, null);
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        stmt.body.visit(this, null);
        stmt.cond.visit(this, null);
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.expr.visit(this, null);
        expr.operator.visit(this, null);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        expr.left.visit(this, null);
        expr.operator.visit(this, null);
        expr.right.visit(this, null);
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        expr.ref.visit(this, null);
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.ixExpr.visit(this, null);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        for (Expression e : expr.argList)
            e.visit(this, null);
        expr.functionRef.visit(this, null);
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        expr.lit.visit(this, null);
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        expr.classtype.visit(this, null);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.eltType.visit(this, null);
        expr.sizeExpr.visit(this, null);
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        if (ref.declaration== null)
            System.out.println("No declaration");
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        ref.id.visit(this, null);
        if (ref.declaration == null){
            System.out.println("No declaration");
        }
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        ref.id.visit(this, null);
        ref.ref.visit(this, null);
        if (ref.declaration== null){
            System.out.println("No declaration");
        }
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        if (id.declaration== null){
            System.out.println("No declaration");
        }
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nullLiteral, Object arg) {
        return null;
    }
}
