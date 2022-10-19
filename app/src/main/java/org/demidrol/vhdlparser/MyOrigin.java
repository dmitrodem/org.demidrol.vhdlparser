package org.demidrol.vhdlparser;

abstract class MyOrigin implements MyJSON {
    public String filename = null;
    public Integer line_start = -1;
    public Integer line_stop  = -1;

    public MyOrigin() {
    }
}
