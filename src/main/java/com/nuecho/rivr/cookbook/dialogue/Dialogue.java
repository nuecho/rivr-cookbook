/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

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
        variables.addWithExpression("sessionId", "session.com.genesyslab.sessionid");

        Script script = new Script("get-session-id");
        script.setVariables(variables);

        VoiceXmlInputTurn inputTurn = DialogueUtils.doTurn(script, context);

        JsonObject result = (JsonObject) inputTurn.getJsonValue();
        context.getLogger().info("Session ID: " + result.getString("sessionId"));

        //end of dialogue
        return new Exit("exit");
    }

}
