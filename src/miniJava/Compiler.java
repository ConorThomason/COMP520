package miniJava;

import mJAM.ObjectFile;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGenerator.Encoder;
import miniJava.ContextualAnalyzer.Identification;
import miniJava.ContextualAnalyzer.TypeChecking;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.AbstractSyntaxTrees.*;

import java.io.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Compiler {
    public static void main(String[] args) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(args[0]);
        }
        catch (FileNotFoundException e) {
            System.out.println("Input file " + args[0] + " not found");
            System.exit(1);
        }
        ErrorReporter errorReporter = new ErrorReporter();
        Scanner scanner = new Scanner(inputStream, errorReporter);
        Parser parser = new Parser(scanner, errorReporter);

        System.out.println("Syntactic analysis ... ");
        Package ast = parser.parse();
        ASTDisplay display = new ASTDisplay();

        System.out.print("Syntactic analysis complete:  ");
        System.out.println("\nContextual Analysis ...");
        TypeChecking typeChecker = new TypeChecking(ast, errorReporter);
        System.out.println("Contextual analysis complete: ");
        Encoder encoder = new Encoder(errorReporter);
        System.out.println("Code generation...");
        encoder.beginEncode(ast, args[0]);

        if (errorReporter.hasErrors()){
            System.out.println("Invalid miniJava program");
            System.out.println(errorReporter.getErrors());
            System.exit(4);
        }
        else{
            System.out.println("Valid miniJava program");
            String objFileName = args[0].substring(0, args[0].lastIndexOf("."));
            ObjectFile objF = new ObjectFile(objFileName + ".mJAM");
            System.out.println("Writing object code file" + objFileName);
            if (objF.write()){
                System.out.println("Failed to write");
                System.exit(4);
            }
            else{
                System.out.println("Successfully written");
            }
//            display.showTree(ast);
            System.exit(0);
            System.exit(0);
        }
    }
}
