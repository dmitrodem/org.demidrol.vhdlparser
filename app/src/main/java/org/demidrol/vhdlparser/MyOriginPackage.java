package org.demidrol.vhdlparser;

import org.json.simple.JSONObject;

class MyOriginPackage extends MyOrigin {
    public String pkg = null;

    public MyOriginPackage() {
    }

    public MyOriginPackage(String filename,
                          Integer line_start,
                          Integer line_stop,
                          String pkg) {
        this.filename = filename;
        this.line_start = line_start;
        this.line_stop = line_stop;
        this.pkg = pkg;
    }

    public void deserialize(JSONObject obj) throws Exception {

        var kind = (String)obj.get("kind");
        if ((kind == null) || (!kind.equals("package"))) {
            throw new Exception("'kind' should be 'package'");
        }
        this.filename = (String)obj.get("filename");
        this.line_start = ((Long)obj.get("line_start")).intValue();
        this.line_stop = ((Long)obj.get("line_stop")).intValue();
        this.pkg = (String)obj.get("package");
    }

    public JSONObject serialize() {
        var obj = new JSONObject();
        obj.put("filename", filename);
        obj.put("line_start", line_start);
        obj.put("line_stop", line_stop);
        obj.put("kind", "entity");
        obj.put("package", pkg);
        return obj;
    }
}
