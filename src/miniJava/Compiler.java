package miniJava;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.Identification;
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
        Identification identification = new Identification(ast, errorReporter);
        System.out.print("Syntactic analysis complete:  ");
        if (errorReporter.hasErrors()){
            System.out.println("Invalid miniJava program");
            System.exit(4);
        }
        else{
            System.out.println("Valid miniJava program");
            display.showTree(ast);
            System.exit(0);
        }
    }
}
