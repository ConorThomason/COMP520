/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class BaseType extends TypeDenoter
{
    public BaseType(TypeKind t, SourcePosition posn){
        super(t, posn);
    }

    @Override
    public boolean equals(Object obj){
        if ((obj instanceof BaseType && this.typeKind == ((BaseType)obj).typeKind) ||
                ((BaseType)obj).typeKind == TypeKind.ERROR ){
            return true;
        } else{
            return false;
        }
    }
    
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitBaseType(this, o);
    }
}
