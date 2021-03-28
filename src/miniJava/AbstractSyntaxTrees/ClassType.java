/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

import javax.lang.model.type.NullType;

public class ClassType extends TypeDenoter
{
    public ClassType(Identifier cn, SourcePosition posn){
        super(TypeKind.CLASS, posn);
        className = cn;
    }
            
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitClassType(this, o);
    }

    @Override public boolean equals(TypeDenoter type){
        if (type instanceof NullType) return true;
        return (super.equals(type) && className.spelling.equals(((ClassType)type).className.spelling));
    }

    public Identifier className;
}
