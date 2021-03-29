package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;

import java.lang.reflect.Member;
import java.util.*;

public class IdentificationTable {
    public ErrorReporter reporter;

    private HashMap<String, HashNode> allTables;
    private boolean staticContext = false;
    private HashNode currentNode;

    public HashNode getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(HashNode currentNode) {
        this.currentNode = currentNode;
    }

    private MemberDecl lastMethod;

    public IdentificationTable(ErrorReporter reporter){
        this.reporter = reporter;
        this.allTables = new HashMap<String, HashNode>();
    }

    public void changeClassView(String className){
        currentNode = allTables.get(className);
    }

    public void insertClass(String id, Declaration attribute){
        allTables.put(id, new HashNode(id, attribute, null, 0));
        currentNode = allTables.get(id);
    }

    public boolean anyDuplicates(String id){
        HashNode copiedNode = new HashNode(currentNode);
        while (copiedNode != null && copiedNode.getNodeLevel() != 0){
            if (copiedNode.getKey().equals(id)){
                return true;
            }
            copiedNode = copiedNode.getPreviousNode();
        }
        if (copiedNode != null && copiedNode.getKey().equals(id)){
            return true;
        }
        return false;
    }
    public void insert(String id, Declaration attribute){

        if (currentNode != null){
            if (!anyDuplicates(id)) {
                currentNode.getNextLevel().put(id, new HashNode(id, attribute, null,
                        currentNode.getNodeLevel() + 1));
                currentNode = currentNode.getNextLevel().get(id);
            }
        }
        else{
            insertClass(id, attribute);
        }
    }
    public Declaration findLocal(String className){
        HashNode copyNode = new HashNode(currentNode);
        while (copyNode != null) {
            if (copyNode.getKey().equals(className)) {
                return copyNode.getNodeDeclaration();
            }
            copyNode = copyNode.getPreviousNode();
        }
        return null;
    }

    public boolean isStaticContext(){
        return currentNode.isStatic();
    }

    public MemberDecl getCurrentMethod(){
        HashNode copiedNode = new HashNode(currentNode);
        while (copiedNode != null && copiedNode.getNodeLevel() >= 1){
            copiedNode = copiedNode.getPreviousNode();
        }
        if (copiedNode == null || copiedNode.getNodeLevel() != 1){
            return null;
        }
        return (MemberDecl) copiedNode.getNodeDeclaration();
    }

    public int getValue(Declaration c){
        if (c instanceof ClassDecl){
            return 0;
        }
        else if (c instanceof MemberDecl){
            return 1;
        }
        else if (c instanceof ParameterDecl){
            return 2;
        }
        else{
            return 3;
        }
    }
    public Declaration findClass(String id){
        if (allTables.get(id) != null){
            return allTables.get(id).getNodeDeclaration();
        }
        return null;
    }

    public Declaration find(String id){
        //First, check immediate descendents
        if (currentNode.getNextLevel() != null && currentNode.getNextLevel().get(id) != null){
            currentNode = currentNode.getNextLevel().get(id);
            return currentNode.getNodeDeclaration();
        }
        //No luck? Start working through previous nodes
        else{
            while (currentNode.getPreviousNode() != null){
               if (currentNode.getNextLevel().get(id) != null){
                   currentNode = currentNode.getNextLevel().get(id);
                   return currentNode.getNodeDeclaration();
               }
               currentNode = currentNode.getPreviousNode();
            }
            if (currentNode.getKey().equals(id))
                return currentNode.getNodeDeclaration();
        }
        //Last shot is to search all the classes.
        for (int i = 0; i < allTables.size(); i++){
            HashNode node = allTables.get(allTables.keySet().toArray()[i]);
            if (allTables.get(id) != null){
                currentNode = findClassNode(node);
                return allTables.get(id).getNodeDeclaration();
            }
            if (node.getNextLevel().get(id) != null){
                currentNode = findClassNode(node);
                if (currentNode.getNodeDeclaration() instanceof MemberDecl){
                    lastMethod = (MethodDecl) currentNode.getNodeDeclaration();
                }
                return node.getNextLevel().get(id).getNodeDeclaration();
            }
        }
        return null;
    }

    public HashNode findClassNode(HashNode node){
        while (node.getPreviousNode() != null){
            node = node.getPreviousNode();
        }
        return node;
    }
    public Declaration findMethodInClass(String className, String method){
        try {
            return allTables.get(className).getNextLevel().get(method).getNodeDeclaration();
        } catch (NullPointerException e){
            return null;
        }
    }

    public int getLevelDepth(){
        return currentNode.getNodeLevel();
    }

    public String getCurrentClassName(){
        HashNode copiedNode = new HashNode(currentNode);
        while (copiedNode != null && copiedNode.getNodeLevel() != 0){
            copiedNode = copiedNode.getPreviousNode();
        }
        if (copiedNode != null) {
            return copiedNode.getKey();
        }
        else{
            return null;
        }
    }

    public void openScope(){
        //nop
    }

    public void closeScope(){
        //nop
    }

    private void TypeError(String error, SourcePosition pos){
        reporter.reportError("*** line " + pos.start + " ***Type Check error: " + error);
    }
}
