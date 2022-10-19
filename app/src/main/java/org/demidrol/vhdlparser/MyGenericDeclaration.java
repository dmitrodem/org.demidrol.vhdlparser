package org.demidrol.vhdlparser;

import org.json.simple.JSONObject;

public class MyGenericDeclaration implements MyJSON {
    public String name = null;
    public String type = null;
    public String dfl = null;

    public MyGenericDeclaration() {
    }
    public MyGenericDeclaration(String name,
                                String type,
                                String dfl) {
        this.name = name;
        this.type = type;
        this.dfl = dfl;
    }

    public void deserialize(JSONObject obj) {
        this.name = (String)obj.get("name");
        this.type = (String)obj.get("type");
        this.dfl = (String)obj.get("default");
    }

    public JSONObject serialize() {
        var obj = new JSONObject();
        obj.put("name", name);
        obj.put("type", type);
        obj.put("default", dfl);
        return obj;
    }
}
