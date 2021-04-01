package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;

import java.util.HashMap;

public class HashNode {
    private String key;
    //0 = class, 1 = method, 2 = params, 3 = local, 4 = local+
    private int nodeLevel = 0;

    public int getNodeLevel() {
        return nodeLevel;
    }

    public void setNodeLevel(int nodeLevel) {
        this.nodeLevel = nodeLevel;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    private Declaration nodeDeclaration;
    private HashMap<String, HashNode> nextLevel;
    private HashNode previousNode;
    private boolean isStatic = false;
    private boolean isPrivate = false;

    public HashNode(String key, Declaration declaration, HashNode previousNode, int currentLevel){
        this.key = key;
        this.nodeDeclaration = declaration;
        this.previousNode = previousNode;
        this.nodeLevel = currentLevel;
        try {
            if (previousNode != null && previousNode.isStatic)
                this.isStatic = true;
            if (previousNode != null && previousNode.isPrivate)
                this.isPrivate = true;
        } catch (NullPointerException e){
            //nop
        }
        this.nextLevel = new HashMap<String, HashNode>();

    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isStatic(){
        return isStatic;
    }

    public void setIsStatic(boolean isStatic){
        this.isStatic = true;
    }

    public void setIsPrivate(boolean isPrivate){
        this.isPrivate = true;
    }


    public HashNode(String key, Declaration declaration, HashNode previousNode, int currentLevel,
                    boolean isStatic, boolean isPrivate){
        this.key = key;
        this.nodeDeclaration = declaration;
        this.previousNode = previousNode;
        this.nodeLevel = currentLevel;
        this.isStatic = isStatic;
        this.isPrivate = isPrivate;
        this.nextLevel = new HashMap<String, HashNode>();
    }
    public HashNode(HashNode node){
        this.key = node.key;
        this.nodeDeclaration = node.nodeDeclaration;
        this.previousNode = node.previousNode;
        this.nextLevel = node.nextLevel;
        this.nodeLevel = node.nodeLevel;
        this.isStatic = node.isStatic;
        this.isPrivate = node.isPrivate;
    }

    public HashNode getPreviousNode() {
        return previousNode;
    }

    public void setPreviousNode(HashNode previousNode) {
        this.previousNode = previousNode;
    }

    public HashMap<String, HashNode> getNextLevel() {
        return nextLevel;
    }

    public void setNextLevel(HashMap<String, HashNode> nextLevel) {
        this.nextLevel = nextLevel;
    }

    public Declaration getNodeDeclaration() {
        return nodeDeclaration;
    }

    public void setNodeDeclaration(Declaration nodeDeclaration) {
        this.nodeDeclaration = nodeDeclaration;
    }
}
