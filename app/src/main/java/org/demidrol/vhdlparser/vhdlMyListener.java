package org.demidrol.vhdlparser;

import java.util.*;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

public class vhdlMyListener extends vhdlBaseListener {
    public static String getString(ParserRuleContext ctx) {
        var start = ctx.getStart().getStartIndex();
        var stop  = ctx.getStop().getStopIndex();
        var interval = new Interval(start, stop);
        return ctx.start.getInputStream().getText(interval);
    }
    @Override public void enterComponent_declaration(vhdlParser.Component_declarationContext ctx) {

        var sb = new StringBuilder();
        var id = ctx.identifier(0);
        sb.append("component ");
        sb.append(id.getText());
        var generic_clause = ctx.generic_clause();
        var port_clause = ctx.port_clause();
        if (generic_clause != null) {
            List<String> generics_list = new LinkedList<>();
            for (var g : generic_clause.generic_list().interface_constant_declaration()) {
                List<String> generic_ids = new LinkedList<>();
                for (var g_id : g.identifier_list().identifier()) {
                    generic_ids.add(g_id.getText());                 
                }
                var g_type = g.subtype_indication().selected_name(0).identifier().getText();
                var g_decl = String.join(",", generic_ids) + " : " + g_type;
                if (g.VARASGN() != null) {
                    var g_default = g.expression().getText();
                    g_decl += (" := " + g_default);
                }
                generics_list.add(g_decl);
            }
            sb.append(" generic (" + String.join("; ", generics_list) + ")");
        }
        if (port_clause != null) {
            List<String> ports_list = new LinkedList<>();
            for (var p : port_clause.port_list().interface_port_list().interface_port_declaration()) {
                List<String> port_ids = new LinkedList<>();
                for (var p_id : p.identifier_list().identifier()) {
                    port_ids.add(p_id.getText());
                }
                var p_dir = p.signal_mode().getText();
                var p_type = getString(p.subtype_indication());
                var p_decl = String.join(", ", port_ids) + " : " + p_dir + " " + p_type;
                if (p.VARASGN() != null) {
                    var p_default = getString(p.expression());
                    p_decl += " := " + p_default;
                }
                ports_list.add(p_decl);
            }
            sb.append(" port (" + String.join("; ", ports_list) + ")");
        }
        System.out.println(sb.toString());
    }
}

