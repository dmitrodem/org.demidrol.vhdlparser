package org.demidrol.vhdlparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.demidrol.vhdlparser.vhdlParser.Component_declarationContext;
import org.demidrol.vhdlparser.vhdlParser.Package_declarationContext;

public class ComponentDeclarationListener extends vhdlBaseListener {

    // if component is inside package
    private String package_name = null;

    // if component is inside architecture (declarative, blocks, etc)
    private String entity_name = null;
    private String architecture_name = null;

    private List<MyComponentDeclaration> components = new ArrayList<>();

    public static String getString(ParserRuleContext ctx) {
        var start = ctx.getStart().getStartIndex();
        var stop  = ctx.getStop().getStopIndex();
        var interval = new Interval(start, stop);
        return ctx.start.getInputStream().getText(interval);
    }

    public List<MyComponentDeclaration> getComponents() {
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

        var component_id = ctx.identifier(0).getText();
        if (Arrays.asList(ExcludedComponents.components).contains(component_id)) {
            return;
        }
        MyOrigin origin = null;
        if (this.package_name != null) {
            origin = new MyOriginPackage(ctx.start.getInputStream().getSourceName(),
                                    ctx.start.getLine(),
                                    ctx.stop.getLine(),
                                    this.package_name);
        } else if (this.entity_name != null) {
            origin = new MyOriginEntity(ctx.start.getInputStream().getSourceName(),
                    ctx.start.getLine(),
                    ctx.stop.getLine(),
                    this.entity_name,
                    this.architecture_name);
        }

        var generics = new ArrayList<MyGenericDeclaration>();
        if (ctx.generic_clause() != null) {
            for (var g : ctx.generic_clause().generic_list().interface_constant_declaration()) {
                var g_type = getString(g.subtype_indication()).toLowerCase();
                String g_default = (g.VARASGN() == null) ? null : getString(g.expression());
                g.identifier_list().identifier().forEach(id -> {
                        generics.add(new MyGenericDeclaration(id.getText(), g_type, g_default));
                    });
            }
        }

        var ports = new ArrayList<MyPortDeclaration>();
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
                        ports.add(new MyPortDeclaration(id.getText(), p_direction, p_type, p_default));
                    });
            }
        }

        var obj = new MyComponentDeclaration(component_id, origin, generics, ports);
        this.components.add(obj);
    }
}
