package org.demidrol.vhdlparser;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ComponentMapListener extends vhdlBaseListener {

    private List<MyComponentDeclaration> compdb = null;
    private String entity = null;
    private String architechture = null;
    private String filename = null;

    class Replacement {
        public Integer start;
        public Integer stop;
        public String value;

        public String toString() {
            return String.format("[%d:%d] %s", start, stop, value);
        }
    };

    public List<Replacement> replacements = null;
    
    public ComponentMapListener(String compdb) throws Exception {
        super();
        this.replacements = new LinkedList<>();
        if (compdb != null) {
            this.compdb = new ArrayList<>();
            var p = new JSONParser();
            var reader = new FileReader(compdb);
            var obj = (JSONArray) p.parse(reader);
            for (var e : obj) {
                var cmp = new MyComponentDeclaration();
                cmp.deserialize((JSONObject)e);
                this.compdb.add(cmp);
            }
        }
    }

    @Override
    public void enterArchitecture_body(vhdlParser.Architecture_bodyContext ctx) {
        this.entity = ctx.identifier(1).getText().toString().toLowerCase();
        this.architechture = ctx.identifier(0).getText().toString().toLowerCase();
        this.filename = ctx.start.getInputStream().getSourceName();
    }
    @Override
    public void exitArchitecture_body(vhdlParser.Architecture_bodyContext ctx) {
        this.entity = null;
        this.architechture = null;
        this.filename = null;
    }


    @Override
    public void enterComponent_instantiation_statement(vhdlParser.Component_instantiation_statementContext ctx) {

        var use_defaults = false;

        var label = ctx.label_colon().identifier().getText().toString();
        var identifier = ctx.instantiated_unit();
        var identifier_name = identifier.name().getText().toString().toLowerCase();
        assert (identifier.ENTITY() == null);
        assert (identifier.CONFIGURATION() == null);

        var candidates = compdb.stream()
                .filter(x -> x.id.toLowerCase().equals(identifier_name))
                .collect(Collectors.toList());


        var generic_positional = false;
        var port_positional = false;
        var generic_actual = new ArrayList<String>();
        var port_actual = new ArrayList<String>();
        var generic_range = new Integer[2];
        var port_range = new Integer[2];
        var has_generics = false;
        var has_ports = false;
        if ((ctx.generic_map_aspect() != null) &&
                (ctx.generic_map_aspect().association_list() != null)) {
            has_generics = true;
            generic_range[0] = ctx.generic_map_aspect().association_list().start.getStartIndex();
            generic_range[1] = ctx.generic_map_aspect().association_list().stop.getStopIndex();

            var is_positional = false;
            var is_named = false;
            for (var g : ctx.generic_map_aspect().association_list().association_element()) {
                var actual_part = MyUtils.getString(g.actual_part()).toLowerCase();
                generic_actual.add(actual_part);
                if (g.ARROW() != null) {
                    is_named = true;
                } else {
                    is_positional = true;
                }
            }
            assert (is_positional ^ is_named);
            generic_positional = is_positional;
        }
        if ((ctx.port_map_aspect() != null) &&
                (ctx.port_map_aspect().association_list() != null)) {
            has_ports = true;
            port_range[0] = ctx.port_map_aspect().association_list().start.getStartIndex();
            port_range[1] = ctx.port_map_aspect().association_list().stop.getStopIndex();
            var is_positional = false;
            var is_named = false;
            for (var p : ctx.port_map_aspect().association_list().association_element()) {
                var actual_part = MyUtils.getString(p.actual_part()).toLowerCase();
                port_actual.add(actual_part);
                if (p.ARROW() != null) {
                    is_named = true;
                } else {
                    is_positional = true;
                }
            }
            assert (is_positional ^ is_named);
            port_positional = is_positional;
        }

        if (generic_positional || port_positional) {
            var msg = String.format("%s:%d: warning: generic or port positional map",
                                    ctx.start.getInputStream().getSourceName(),
                                    ctx.start.getLine());
            System.err.println(msg);
        }

        if (candidates.isEmpty()) {
            System.err.println("No candidates for " + label + ": " + identifier_name);
            return;
        }

        if (has_generics && generic_positional && (candidates.get(0).generics != null)) {
            var gmap = new ArrayList<String>();
            var formals = candidates.get(0).generics.stream()
                    .map(g -> g.name)
                    .collect(Collectors.toList());
            for (var i = 0; i < formals.size(); i++) {
                var formal = formals.get(i);
                if (i < generic_actual.size()) {
                    var actual = generic_actual.get(i);
                    gmap.add(formal + " => " + actual);
                } else if (use_defaults) {
                    final int idx = i;
                    var dfl = candidates.stream()
                            .map(c -> c.generics.get(idx).dfl)
                            .filter(s -> s != null)
                            .findFirst();
                    if (dfl.isEmpty()) {
                        var msg = String.format(
                                "Cannot find generic default value. Component = %s, formal = %s, line = %d",
                                identifier_name, formal, ctx.start.getLine());
                        throw new RuntimeException(msg);
                    }
                    gmap.add(formal + " => " + dfl.get());
                }
            }
            this.replacements.add(new Replacement() {
                {
                    start = generic_range[0];
                    stop = generic_range[1] + 1;
                    value = String.join(", ", gmap);
                }
            });
        }
        if (has_ports && port_positional && (candidates.get(0).ports != null)) {
            var pmap = new ArrayList<String>();
            var formals = candidates.get(0).ports.stream()
                    .map(p -> p.name)
                    .collect(Collectors.toList());
            var directions = candidates.get(0).ports.stream()
                    .map(p -> p.direction)
                    .collect(Collectors.toList());

            for (var i = 0; i < formals.size(); i++) {
                var formal = formals.get(i);
                var direction = directions.get(i);
                if (i < port_actual.size()) {
                    var actual = port_actual.get(i);
                    pmap.add(formal + " => " + actual);
                } else if (use_defaults) {
                    final int idx = i;
                    var dfl = candidates.stream()
                            .map(c -> c.ports.get(idx).dfl)
                            .filter(s -> s != null)
                            .findFirst();
                    if (dfl.isEmpty()) {
                        if (direction.toLowerCase().equals("out")) {
                            pmap.add(formal + " => open");
                        } else {
                            var msg = String.format(
                                    "Cannot find port default value. Component = %s, formal = %s, line = %d",
                                    identifier_name, formal, ctx.start.getLine());
                            throw new RuntimeException(msg);
                        }
                    } else {
                        pmap.add(formal + " => " + dfl.get());
                    }
                }
            }
            this.replacements.add(new Replacement() {
                {
                    start = port_range[0];
                    stop = port_range[1] + 1;
                    value = String.join(", ", pmap);
                }
            });
        }
    }
};
