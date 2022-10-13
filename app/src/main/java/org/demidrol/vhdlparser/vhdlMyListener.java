package org.demidrol.vhdlparser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.demidrol.vhdlparser.vhdlParser.Component_declarationContext;
import org.demidrol.vhdlparser.vhdlParser.Package_declarationContext;

public class vhdlMyListener extends vhdlBaseListener {

    // if component is inside package
    private String package_name = null;

    // if component is inside architecture (declarative, blocks, etc)
    private String entity_name = null;
    private String architecture_name = null;

    private List<Object> components = new LinkedList<Object>();

    public static String getString(ParserRuleContext ctx) {
        var start = ctx.getStart().getStartIndex();
        var stop  = ctx.getStop().getStopIndex();
        var interval = new Interval(start, stop);
        return ctx.start.getInputStream().getText(interval);
    }

    public List<Object> getComponents() {
        return this.components;
    }

    @Override
    public void enterPackage_declaration(Package_declarationContext ctx) {
        this.package_name = ctx.identifier(0).getText();
    }

    @Override
    public void exitPackage_declaration(Package_declarationContext ctx) {
        this.package_name = null;
    }

    @Override
    public void enterArchitecture_body(vhdlParser.Architecture_bodyContext ctx) {
        var len = ctx.identifier().size();
        assert (len == 2) ||
            ((len == 3) &&
             (ctx.identifier(0).getText().toLowerCase() == ctx.identifier(2).getText().toLowerCase()));
        this.entity_name = ctx.identifier(1).getText().toLowerCase();
        this.architecture_name = ctx.identifier(1).getText().toLowerCase();
    }

    @Override
    public void exitArchitecture_body(vhdlParser.Architecture_bodyContext ctx) {
        this.entity_name = null;
        this.architecture_name = null;
    }

    @Override public void enterComponent_declaration(Component_declarationContext ctx) {

        assert
            (this.package_name != null) ||
            (this.entity_name != null);

        var obj = new HashMap<String, Object>();

        assert ctx.identifier().size() == 1;
        obj.put("id", ctx.identifier(0).getText());

        var origin = new HashMap<String, Object>();
        origin.put("filename", ctx.start.getInputStream().getSourceName());
        origin.put("line_start", ctx.start.getLine());
        origin.put("line_stop", ctx.stop.getLine());
        if (this.package_name != null) {
            origin.put("kind", "package");
            origin.put("package", this.package_name);
        } else if (this.entity_name != null) {
            origin.put("kind", "entity");
            origin.put("entity", this.entity_name);
            origin.put("architecture", this.architecture_name);
        }
        obj.put("origin", origin);

        var generics = new LinkedList<Object>();
        if (ctx.generic_clause() != null) {
            for (var g : ctx.generic_clause().generic_list().interface_constant_declaration()) {
                var g_type = getString(g.subtype_indication()).toLowerCase();
                String g_default = (g.VARASGN() == null) ? null : getString(g.expression());
                g.identifier_list().identifier().forEach(id -> {
                        var generic = new HashMap<String, Object>();
                        generic.put("name", id.getText());
                        generic.put("type", g_type);
                        generic.put("default", g_default);
                        generics.add(generic);
                    });
            }
        }
        obj.put("generics", generics);

        var ports = new LinkedList<Object>();
        if (ctx.port_clause() != null) {
            for (var p :
                     ctx.port_clause()
                     .port_list()
                     .interface_port_list()
                     .interface_port_declaration()) {
                var p_type = getString(p.subtype_indication());
                var p_direction = (p.signal_mode() == null) ? "in" : getString(p.signal_mode()).toLowerCase();
                var p_default = (p.VARASGN() == null) ? null : getString(p.expression()).toLowerCase();
                p.identifier_list().identifier().forEach(id -> {
                    var port = new HashMap<String, Object>();
                    port.put("name", id.getText());
                    port.put("type", p_type);
                    port.put("direction", p_direction);
                    port.put("default", p_default);
                    ports.add(port);
                });
            }
        }
        obj.put("ports", ports);

        this.components.add(obj);
    }
}
