package org.demidrol.vhdlparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

class MyComponentDeclaration implements MyJSON {
    public String id = null;
    public MyOrigin origin = null;
    public List<MyGenericDeclaration> generics = null;
    public List<MyPortDeclaration> ports = null;

    public MyComponentDeclaration() {
    }

    public MyComponentDeclaration(String id,
                                  MyOrigin origin,
                                  List<MyGenericDeclaration> generics,
                                  List<MyPortDeclaration> ports) {
        this.id = id;
        this.origin = origin;
        this.generics = generics;
        this.ports = ports;
    }


    public void deserialize(JSONObject obj) throws Exception {
        this.id = (String) obj.get("id");
        var kind = ((HashMap<String, String>) obj.get("origin")).get("kind");
        if (kind.equals("entity")) {
            this.origin = new MyOriginEntity();
        } else if (kind.equals("package")) {
            this.origin = new MyOriginPackage();
        }
        this.origin.deserialize((JSONObject) obj.get("origin"));

        this.generics = new ArrayList<MyGenericDeclaration>();
        for (var g : (List<JSONObject>)obj.get("generics")) {
            var generic = new MyGenericDeclaration();
            generic.deserialize(g);
            this.generics.add(generic);
        }

        this.ports = new ArrayList<MyPortDeclaration>();
        for (var p : (List<JSONObject>) obj.get("ports")) {
            var port = new MyPortDeclaration();
            port.deserialize(p);
            this.ports.add(port);
        }
    }

    public JSONObject serialize() {
        var obj = new JSONObject();
        obj.put("id", this.id);
        obj.put("origin", this.origin.serialize());

        var obj_generics = new JSONArray();
        for (var g : this.generics) {
            obj_generics.add(g.serialize());
        }
        obj.put("generics", obj_generics);

        var obj_ports = new JSONArray();
        for (var p : this.ports) {
            obj_ports.add(p.serialize());
        }
        obj.put("ports", obj_ports);

        return obj;
    }
}
