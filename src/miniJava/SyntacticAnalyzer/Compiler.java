package miniJava.SyntacticAnalyzer;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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
        parser.parse();
        System.out.print("Syntactic analysis complete:  ");
        if (errorReporter.hasErrors()){
            System.out.println("Invalid miniJava program");
            System.exit(4);
        }
        else{
            System.out.println("Valid miniJava program");
            System.exit(0);
        }
    }
}
