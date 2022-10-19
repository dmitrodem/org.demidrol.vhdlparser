package org.demidrol.vhdlparser;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


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

    public static void createComponentDatabase(String path, String output) throws Exception {
        List<MyComponentDeclaration> components = new ArrayList<>();
        var vhdlFiles = findFilesByExtension(Paths.get(path), ".vhd");
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
        var json = new JSONArray();
        for (var e : components) {
            json.add(e.serialize());
        }
        var fd = new FileWriter(output);
        fd.write(json.toJSONString());
        fd.close();
    }

    public static void findDuplicates(String compdb) throws Exception {
        var parser = new JSONParser();
        var reader = new FileReader(compdb);
        var obj = (JSONArray) parser.parse(reader);
        var components = new ArrayList<MyComponentDeclaration>();
        for (var e : obj) {
            var cmp = new MyComponentDeclaration();
            cmp.deserialize((JSONObject)e);
            components.add(cmp);
        }
        var all_components = new HashSet<String>();
        for (var e : components) {
            all_components.add(e.id.toLowerCase());
        }

        for (var ref : all_components) {
            var problems = new ArrayList<String>();
            boolean ok = true;

            var duplicates = new ArrayList<MyComponentDeclaration>();
            for (var cmp : components) {
                var c = cmp.id.toLowerCase();
                if (c.equals(ref)) {
                    duplicates.add(cmp);
                }
            }
            var nports = new HashSet<Integer>();
            var ngenerics = new HashSet<Integer>();

            for (var e : duplicates) {
                nports.add(e.ports.size());
                ngenerics.add(e.generics.size());
            }
            if (nports.size() > 1) {
                problems.add("different number of ports = " + nports);
                ok = false;
            }
            if (ngenerics.size() > 1) {
                problems.add("different number of generics = " + ngenerics);
                ok = false;
            }
            if (nports.size() == 1) {
                var lports = new ArrayList<Integer>(nports).get(0);
                for (int n = 0; n < lports; n++) {
                    var names = new HashSet<String>();
                    var directions = new HashSet<String>();
                    var types = new HashSet<String>();
                    var defaults = new HashSet<String>();
                    for (var e : duplicates) {
                        var port = e.ports.get(n);
                        names.add(port.name.toLowerCase());
                        directions.add(port.direction.toLowerCase());
                        types.add(port.type.toLowerCase());
                        var dfl = port.dfl;
                        if (dfl != null) {
                            defaults.add(dfl.toLowerCase());
                        }
                    }
                    if (names.size() > 1) {
                        problems.add("inconsistent port names (" + n + ") " + names);
                        ok = false;
                    }
                    if (directions.size() > 1) {
                        problems.add("inconsistent port directions (" + n + ") " + directions);
                        ok = false;
                    }
                    if (types.size() > 1) {
                        problems.add("inconsistent port types (" + n + ") " + types);
                        ok = false;
                    }
                    if (defaults.size() > 1) {
                        problems.add("inconsistent port defaults (" + n + ") " + defaults);
                        ok = false;
                    }
                }
            }
            if (ngenerics.size() == 1) {
                var lgenerics = new ArrayList<Integer>(ngenerics).get(0);
                for (int n = 0; n < lgenerics; n++) {
                    var names = new HashSet<String>();
                    var types = new HashSet<String>();
                    var defaults = new HashSet<String>();
                    for (var e : duplicates) {
                        var generic = e.generics.get(n);
                        names.add(generic.name.toLowerCase());
                        types.add(generic.type.toLowerCase());
                        var dfl = generic.dfl;
                        if (dfl != null) {
                            defaults.add(dfl.toLowerCase());
                        }

                    }
                    if (names.size() > 1) {
                        problems.add("inconsistent generic names (" + n + ") " + names);
                        ok = false;
                    }
                    if (types.size() > 1) {
                        problems.add("inconsistent generic types (" + n + ") " + types);
                        ok = false;
                    }
                    if (defaults.size() > 1) {
                        problems.add("inconsistent generic defaults (" + n + ") " + defaults);
                        ok = false;
                    }
                }
            }
            if (ok) {
                // check if component has declaration in package and advise to use it
                String package_origin = null;
                for (var e : duplicates) {
                    
                    if (e.origin instanceof MyOriginPackage) {
                        package_origin = ((MyOriginPackage)e.origin).pkg;
                        break;
                    }
                }
                if (package_origin != null) {
                    for (var e: duplicates) {
                        if (!(e.origin instanceof MyOriginPackage)) {
                            var origin = e.origin;
                            System.out.println(origin.filename + ":" + origin.line_start +
                                               ": prefer declaration '" +
                                               e.id +
                                               "' from package " + package_origin);
                        }
                    }
                }
            }
            if (!ok) {
                System.out.println("REF " + ref);
                for (var e : duplicates) {
                    System.out.println(e.origin.filename + ":" + e.origin.line_start + ": error: some problems");
                }
                System.out.println("problems");
                System.out.println(String.join("\n", problems));
            }
        }           
    }


    public static void findComponentInstantiations(String path, String compdb) throws Exception {
        var vhdlFiles = findFilesByExtension(Paths.get(path), ".vhd");
        for (var vhdlFile : vhdlFiles) {
            System.out.println(vhdlFile);
            var input = CharStreams.fromPath(vhdlFile);
            var lexer = new vhdlLexer(input);
            var tokens = new CommonTokenStream(lexer);
            var parser = new vhdlParser(tokens);
            var tree = parser.design_file();
            var walker = new ParseTreeWalker();
            var listener = new ComponentMapListener(compdb);
            walker.walk(listener, tree);
            if (!listener.replacements.isEmpty()) {
                var vhdlOut = vhdlFile.getParent().resolve(vhdlFile.getFileName() + ".new");
                var output = new FileWriter(vhdlOut.toFile());
                var finput = new FileReader(vhdlFile.toFile());

                Integer stop = 0;
                for (var replacement : listener.replacements) {
                    var buf = new char[replacement.start - stop];
                    var xbuf = new char[replacement.stop - replacement.start];

                    var r1 = finput.read(buf);
                    var r2 = finput.read(xbuf);

                    assert (r1 == replacement.start - stop);
                    assert (r2 == replacement.stop - replacement.start);

                    output.write(buf);
                    output.write(replacement.value);
                    
                    stop = replacement.stop;
                }
                var xstop = (int) Files.size(vhdlFile);
                var buf = new char[xstop - stop];
                finput.read(buf);
                output.write(buf);

                finput.close();
                output.close();
            }
        }

    }

    public static void main(String[] args) throws Exception {
        var options = new Options();

        options.addOption("h", "help", false, "Display help");

        var opt_libdir = new Option("l", "libdir", true, "GRLIB lib directory");
        opt_libdir.setRequired(true);
        options.addOption(opt_libdir);

        var opt_compdb = new Option("c", "compdb", true, "Component database (json)");
        //opt_compdb.setRequired(true);
        options.addOption(opt_compdb);

        options.addOption(null, "database", false, "Create component database");
        options.addOption(null, "verify", false, "Verify component database");
        options.addOption(null, "installations", false, "Find component installations");

        var cmd_parser = new DefaultParser();
        var help_formatter = new HelpFormatter();
        var cmd = (CommandLine) null;

        try {
            cmd = cmd_parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            help_formatter.printHelp("App", options);
            System.exit(-1);
        }

        if (cmd.hasOption("help")) {
            help_formatter.printHelp("App", options);
            System.exit(-1);
        }

        var libdir = cmd.getOptionValue("libdir");
        var compdb = cmd.getOptionValue("compdb");

        if (cmd.hasOption("database")) {
            if ((libdir == null) || (compdb == null)) {
                throw new RuntimeException("libdir or compdb are null");
            }
            createComponentDatabase(libdir, compdb);
        }

        if (cmd.hasOption("verify")) {
            if (compdb == null) {
                throw new RuntimeException("compdb is null");
            }
            findDuplicates(compdb);
        }

        if (cmd.hasOption("installations")) {
            findComponentInstantiations(libdir, compdb);
        }

        // System.out.println("libdir = " + libdir + ", compdb = " + compdb);
    }
}
