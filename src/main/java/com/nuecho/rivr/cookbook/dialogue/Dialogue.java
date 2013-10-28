/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import java.util.Map.Entry;

import javax.json.*;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;

public class Dialogue implements VoiceXmlDialogue {

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
            throws Exception {

        VariableList variables = new VariableList();
        variables.addWithExpression("userData", "session.com.genesyslab.userdata");

        Script script = new Script("read-user-data");
        script.setVariables(variables);

        VoiceXmlInputTurn inputTurn = DialogueUtils.doTurn(script, context);

        JsonObject result = (JsonObject) inputTurn.getJsonValue();
        JsonValue userDataValue = result.get("userData");

        if (userDataValue instanceof JsonObject) {
            JsonObject userData = (JsonObject) userDataValue;
            context.getLogger().info("User data:");
            for (Entry<String, JsonValue> entry : userData.entrySet()) {
                context.getLogger().info("  " + entry.getKey() + "=" + entry.getValue().toString());
            }
        } else {
            context.getLogger().info("No user data found.");
        }

        //end of dialogue
        return new Exit("exit");
    }

}
