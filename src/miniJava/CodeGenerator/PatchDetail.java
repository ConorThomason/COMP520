package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.Declaration;

public class PatchDetail {
    public final int address;
    public final Declaration declaration;
    public PatchDetail(int address, Declaration declaration){
        this.address = address;
        this.declaration = declaration;
    }
}
