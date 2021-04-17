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
    public ArrayList<String> predefinedClasses = new ArrayList<>();
    public Stack<HashNode> savedContext = new Stack<>();

    public HashNode getSavedContext(){
        try {
            return this.savedContext.peek();
        } catch (EmptyStackException e){
            return null;
        }
    }
    public HashMap<String, HashNode> getAllTables(){
        return this.allTables;
    }
    public boolean isPredefined(String className){
        if (predefinedClasses.contains(className)) {
            return true;
        }
        return false;
    }
    public HashNode getCurrentNode() {
        return currentNode;
    }

    private MemberDecl lastMethod;

    public IdentificationTable(ErrorReporter reporter){
        this.reporter = reporter;
        this.allTables = new HashMap<String, HashNode>();
        String[] predefined = {"System", "_PrintStream", "String", "out", "println"};
        this.predefinedClasses.addAll(Arrays.asList(predefined));
    }

    public String searchPredefined(String id){
        for (String a : predefinedClasses){
            if (allTables.get(a) != null){
                if (findLevel1FromClass(id, a) != null){
                   return a;
                }
            }
        }
        return null;
    }

    public void insertClass(String id, Declaration attribute){
        allTables.put(id, new HashNode(id, attribute, null, 0));
        currentNode = allTables.get(id);
    }
    public void insert(String id, Declaration attribute){
        if (currentNode == null){
            allTables.putIfAbsent(id, new HashNode(id, attribute, null, 0,
                    false, false));
            currentNode = allTables.get(id);
        }
        else{
            if (aWiderScope(currentNode.getNodeDeclaration(), attribute) == 1){
                    currentNode.getNextLevel().put(id, new HashNode(id, attribute,
                    currentNode, currentNode.getNodeLevel() + 1, currentNode.isStatic(),
                    currentNode.isPrivate()));
                    currentNode = currentNode.getNextLevel().get(id);

            }
            else if (aWiderScope(currentNode.getNodeDeclaration(), attribute) == 0){
                if (allTables.get(currentNode.getKey()) != null) {
                    currentNode.getNextLevel().put(id, new HashNode(id, attribute, currentNode,
                            currentNode.getNodeLevel() + 1, currentNode.isStatic(), currentNode.isPrivate()));
                    currentNode = currentNode.getNextLevel().get(id);
                }
                else if (currentNode.getNodeLevel() >= 1){
                    currentNode.getPreviousNode().getNextLevel().put(id, new HashNode(id, attribute, currentNode.getPreviousNode(),
                            currentNode.getNodeLevel(), currentNode.isStatic(), currentNode.isPrivate()));
                    currentNode = currentNode.getPreviousNode().getNextLevel().get(id);
                }
                else{
                    allTables.put(id, new HashNode(id, attribute, null, 0, false, false));
                }
            }
            else if (aWiderScope(currentNode.getNodeDeclaration(), attribute) == -1){
                currentNode = currentNode.getPreviousNode();
                insert(id, attribute);
            }
        }
    }

    public int aWiderScope(Declaration a, Declaration b) {
        int aResult = valueConversion(a);
        int bResult = valueConversion(b);

        if (aResult < bResult) {
            return 1;
        } else if (aResult == bResult) {
            return 0;
        } else {
            return -1;
        }
    }

    public HashNode downSearchNoMove(String search){
        if (currentNode.getNextLevel().get(search) != null){
            return currentNode.getNextLevel().get(search);
        }
        return null;
    }
    public int valueConversion(Declaration a){
        if (a instanceof ClassDecl){
            return 0;
        } else if (a instanceof MemberDecl){
            return 1;
        } else if (a instanceof ParameterDecl){
            return 2;
        } else if (a instanceof LocalDecl){
            return 3;
        }
        else{
        return 4;
        }
    }

    public void changeClassContext(HashNode className){
        this.savedContext.push(this.currentNode);
        this.currentNode = className;
    }

    public void returnFromContext(){
        try {
            this.currentNode = this.savedContext.pop();
        } catch (EmptyStackException e){
            //nop
        }
    }
    public Declaration findLevel1(){
        HashNode differentClass = findClassNode(currentNode);
        if (differentClass != null){
            HashNode differentMethod = differentClass.getNextLevel().get(currentNode);
            if (differentMethod != null){
                return differentMethod.getNodeDeclaration();
            }
        }
        return null;
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

    public HashNode findClassNode(String id){
        if (allTables.get(id) != null){
            return allTables.get(id);
        }
        return null;
    }

    public HashNode findLevel1NodeFromClass(String method, String className){
        HashNode differentClass = findClassNode(className);
        if (differentClass != null){
            HashNode differentMethod = differentClass.getNextLevel().get(method);
            if (differentMethod != null){
                return differentMethod;
            }
        }
        return null;
    }

    public Declaration findLevel1FromClass(String method, String className){
        HashNode differentClass = findClassNode(className);
        if (differentClass != null){
            HashNode differentMethod = differentClass.getNextLevel().get(method);
            if (differentMethod != null){
                return differentMethod.getNodeDeclaration();
            }
        }
        return null;
    }

    public Declaration find(String id){
        //I'm... not quite sure yet if I want this to adjust the position every time I find. TBD.
        //Yes, adjusting the position seems to be the way to go

        if (currentNode != null){
            //If current node is empty, there's shouldn't be anything in the structure anyway
            //So yes, we can just skip over it using the if statement

            //First case - if the currentNode already has what we need
            if (currentNode.getKey().equals(id)){
                return currentNode.getNodeDeclaration();
            }
            //Second case - It doesn't have what we need, so we need to do the only thing
            //we can do, which is to go bottom-up, searching on each tier until we find something,
            //or until we reach the Class level and have to stop. At which point...
            else{
                HashNode copyNode = new HashNode(currentNode);
                while (copyNode != null){
                    if (copyNode.getNextLevel().get(id) != null){
                        currentNode = copyNode.getNextLevel().get(id);
                        return copyNode.getNextLevel().get(id).getNodeDeclaration();
                    }
                    copyNode = copyNode.getPreviousNode();
                }
                //Third case - We check allTables to see if we get any returns. If we do, we need to switch
                //contexts, and we need to keep a fairly close track of it - that'll be handled on
                //the TypeChecking side for now, may change as work continues.
                if (allTables.get(id) != null) {
                    return allTables.get(id).getNodeDeclaration();
                }

            }
        }
        return null;
    }

    public HashNode findNodeNoChange(String id){
        //I'm... not quite sure yet if I want this to adjust the position every time I find. TBD.
        //Yes, adjusting the position seems to be the way to go

        if (currentNode != null){
            //If current node is empty, there's shouldn't be anything in the structure anyway
            //So yes, we can just skip over it using the if statement

            //First case - if the currentNode already has what we need
            if (currentNode.getKey().equals(id)){
                return currentNode;
            }
            //Second case - It doesn't have what we need, so we need to do the only thing
            //we can do, which is to go bottom-up, searching on each tier until we find something,
            //or until we reach the Class level and have to stop. At which point...
            else{
                HashNode copyNode = new HashNode(currentNode);
                while (copyNode != null){
                    if (copyNode.getNextLevel().get(id) != null){
                        return copyNode.getNextLevel().get(id);
                    }
                    copyNode = copyNode.getPreviousNode();
                }
                //Third case - We check allTables to see if we get any returns. If we do, we need to switch
                //contexts, and we need to keep a fairly close track of it - that'll be handled on
                //the TypeChecking side for now, may change as work continues.
                if (allTables.get(id) != null) {
                    return allTables.get(id);
                }

            }
        }
        return null;
    }

    public void goToClass(){
        while (currentNode.getPreviousNode() != null){
            currentNode = currentNode.getPreviousNode();
        }
    }
    public HashNode findClassNode(HashNode node){
        if (node != null) {
            while (node.getPreviousNode() != null) {
                node = node.getPreviousNode();
            }
            return node;
        }
        return null;
    }

    public int getLevelDepth(){
        if (currentNode != null)
        return currentNode.getNodeLevel();
        return -1;
    }

    public String getCurrentClassName(){
        if (currentNode == null){
            return null;
        }
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
