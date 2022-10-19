package org.demidrol.vhdlparser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

public class MyUtils {
    public static String getString(ParserRuleContext ctx) {
        var start = ctx.getStart().getStartIndex();
        var stop  = ctx.getStop().getStopIndex();
        var interval = new Interval(start, stop);
        return ctx.start.getInputStream().getText(interval);
    }
}
