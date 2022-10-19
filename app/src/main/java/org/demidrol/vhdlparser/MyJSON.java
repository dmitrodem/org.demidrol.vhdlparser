package org.demidrol.vhdlparser;

import org.json.simple.JSONObject;

interface MyJSON {
    public void deserialize(JSONObject obj) throws Exception;
    public JSONObject serialize();
}
