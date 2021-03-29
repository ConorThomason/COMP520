package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.ErrorReporter;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Stack;

public class IdentificationTable {
    public ErrorReporter reporter;

    private HashMap<String, Declaration> latestEntry;
    private Stack<HashMap<String, Declaration>> allTables;
    private boolean staticContext = false;

    public IdentificationTable(ErrorReporter reporter){
        this.reporter = reporter;
        this.allTables = new Stack<>();
        this.latestEntry = new HashMap<>();
    }
    public void insertClass(String id, Declaration attribute){
        insert(id, attribute);
    }
    public int insert(String id, Declaration attribute){
        latestEntry = allTables.peek();
        if (latestEntry.get(id) != null){
            System.out.println("Duplicate Found, ID: " + id + " Attribute: " + attribute);
            //TODO Print Table
            System.out.println(latestEntry);
            System.exit(4);
            return 0;
        }
        //i >= 3 -> Greater than or equal to the "var" levels.
        Stack<HashMap<String, Declaration>> copiedStack =
                (Stack<HashMap<String, Declaration>>)allTables.clone();

        for (int i = getLevelDepth() - 1; i >= 3; i--){
            if (copiedStack.pop().get(id) != null){
                System.out.println("Duplicate Found, ID: " + id + " Attribute: " + attribute);
                //TODO Print Table
                System.exit(4);
                return 0;
            }
        }
        if (attribute instanceof MemberDecl && getLevelDepth() == 2 && ((MemberDecl)attribute).isStatic){
            if (Identification.debug){
                System.out.println("Context is static");
            }
            staticContext = true;
        }
        latestEntry.put(id, attribute);
        allTables.pop();
        allTables.push(latestEntry);
        return 1;
    }

    public Declaration findMethodInClass(String className, String method){
        for (HashMap<String, Declaration> h : allTables){
            if (h.get(className) != null){
                if (h.get(className))
            }
            else{

            }
        }
    }

    public Declaration find(String id){
        Declaration attribute = null;
        Stack<HashMap<String, Declaration>> copiedStack =
                (Stack<HashMap<String, Declaration>>)allTables.clone();
        for (int i = getLevelDepth() - 1; i >=0; i--){
            attribute = copiedStack.pop().get(id);
            if (attribute != null){
                return attribute;
            }
        }
        System.out.println("ID: " + id + " not found at " + getLevelDepth());
        return null;
    }

    public int getLevelDepth(){
        return allTables.size();
    }

    public void openScope(){
        if (Identification.debug) System.out.println("Opening Scope");
        latestEntry = new HashMap<String, Declaration>();
        allTables.push(latestEntry);
    }

    public void openScope(HashMap<String, Declaration> levelTable){
        latestEntry = levelTable;
        allTables.push(latestEntry);
    }

    public boolean isStaticContext(){
        return staticContext;
    }

    public void closeScope(){
        if (isStaticContext() && getLevelDepth() == 1){
            if (Identification.debug) System.out.println("Dropping static context");
            staticContext = false;
        }
        HashMap<String, Declaration> value = allTables.pop();
        if (Identification.debug) System.out.println("Closing scope, " + value + " popped");
    }
}