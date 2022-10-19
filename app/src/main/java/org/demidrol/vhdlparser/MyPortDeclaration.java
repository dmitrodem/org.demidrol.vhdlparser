package org.demidrol.vhdlparser;

import org.json.simple.JSONObject;

public class MyPortDeclaration implements MyJSON {
    public String name = null;
    public String direction = null;
    public String type = null;
    public String dfl = null;

    public MyPortDeclaration() {
    }
    public MyPortDeclaration(String name,
                             String direction,
                             String type,
                             String dfl) {
        this.name = name;
        this.direction = direction;
        this.type = type;
        this.dfl = dfl;
    }

    public void deserialize(JSONObject obj) {
        this.name = (String)obj.get("name");
        this.direction = (String)obj.get("direction");
        this.type = (String)obj.get("type");
        this.dfl = (String)obj.get("default");
    }

    public JSONObject serialize() {
        var obj = new JSONObject();
        obj.put("name", name);
        obj.put("direction", direction);
        obj.put("type", type);
        obj.put("default", dfl);
        return obj;
    }
}
