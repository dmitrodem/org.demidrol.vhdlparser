package org.demidrol.vhdlparser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.FileInputStream;
import java.io.InputStream;


public class App {

    public static void main(String[] args) throws Exception {        
        String inputFile = null;
        if ( args.length>0 ) inputFile = args[0];
        InputStream is = System.in;
        if ( inputFile!=null ) is = new FileInputStream(inputFile);
        ANTLRInputStream input = new ANTLRInputStream(is);
        vhdlLexer lexer = new vhdlLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        vhdlParser parser = new vhdlParser(tokens);
        ParseTree tree = parser.design_file();
        ParseTreeWalker walker = new ParseTreeWalker();

        vhdlListener listener = new vhdlMyListener();

        walker.walk(listener, tree);
    }
}
