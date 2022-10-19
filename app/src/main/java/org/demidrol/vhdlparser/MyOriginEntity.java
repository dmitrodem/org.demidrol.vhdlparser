package org.demidrol.vhdlparser;

import org.json.simple.JSONObject;

class MyOriginEntity extends MyOrigin {
    public String entity = null;
    public String architecture = null;

    public MyOriginEntity() {
    }

    public MyOriginEntity(String filename,
                          Integer line_start,
                          Integer line_stop,
                          String entity,
                          String architecture) {
        this.filename = filename;
        this.line_start = line_start;
        this.line_stop = line_stop;
        this.entity = entity;
        this.architecture = architecture;
    }

    public void deserialize(JSONObject obj) throws Exception {

        var kind = (String)obj.get("kind");
        if ((kind == null) || (!kind.equals("entity"))) {
            throw new Exception("'kind' should be 'entity'");
        }
        this.filename = (String)obj.get("filename");
        this.line_start = ((Long)obj.get("line_start")).intValue();
        this.line_stop = ((Long)obj.get("line_stop")).intValue();
        this.entity = (String)obj.get("entity");
        this.architecture = (String)obj.get("architecture");
    }

    public JSONObject serialize() {
        var obj = new JSONObject();
        obj.put("filename", filename);
        obj.put("line_start", line_start);
        obj.put("line_stop", line_stop);
        obj.put("kind", "entity");
        obj.put("entity", entity);
        obj.put("architecture", architecture);
        return obj;
    }
}
