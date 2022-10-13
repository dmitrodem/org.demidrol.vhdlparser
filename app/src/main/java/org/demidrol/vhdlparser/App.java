package org.demidrol.vhdlparser;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.json.simple.JSONObject;


public class App {

    public static List<Path> findFilesByExtension(Path path, String fileExtension) throws IOException {
        if (! Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path must be a directory");
        }
        List<Path> result;
        try (var walk = Files.walk(path)) {
            result = walk
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(fileExtension))
                .filter(p -> !p.getFileName().toString().endsWith(".in" + fileExtension))
                .filter(p -> !p.getFileName().toString().matches("gr(l?)fpw_(p?)[0-9]_.*"))
                .filter(p -> !p.getFileName().toString().matches("mul_unisim.*"))
                .collect(Collectors.toList());
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        List<Object> components = new LinkedList<Object>();
        var vhdlFiles = findFilesByExtension(Paths.get(args[0]), ".vhd");
        for (var vhdlFile : vhdlFiles) {
            System.out.println(vhdlFile);
            var input = CharStreams.fromPath(vhdlFile);
            var lexer = new vhdlLexer(input);
            var tokens = new CommonTokenStream(lexer);
            var parser = new vhdlParser(tokens);
            var tree = parser.design_file();
            var walker = new ParseTreeWalker();
            var listener = new vhdlMyListener();
            walker.walk(listener, tree);
            components = Stream.of(components, listener.getComponents())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        }
        var json = new JSONObject();
        json.put("components", components);
        var fd = new FileWriter("components.json");
        fd.write(json.toJSONString());
        fd.close();
    }
}
