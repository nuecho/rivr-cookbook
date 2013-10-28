/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import java.util.Map.Entry;

import javax.json.*;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.rendering.voicexml.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.SubdialogueCall.Parameter;

public class Dialogue implements VoiceXmlDialogue {

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
            throws Exception {

        SubdialogueCall subdialogueCall = new SubdialogueCall("invoke-subdialogue", "subdialogue.vxml");

        subdialogueCall.setMethod(SubmitMethod.post);

        Parameter parameter1 = Parameter.createWithValue("parameter1", "string");
        Parameter parameter2 = Parameter.createWithExpression("parameter2", "2+3");
        Parameter parameter3 = Parameter.createWithExpression("parameter3", "'string'");
        subdialogueCall.setSubdialogueParameters(parameter1, parameter2, parameter3);

        VoiceXmlInputTurn inputTurn = DialogueUtils.doTurn(subdialogueCall, context);

        if (inputTurn.getJsonValue() != null) {
            JsonObject subdialogueReturnValues = (JsonObject) inputTurn.getJsonValue();

            for (Entry<String, JsonValue> returnValue : subdialogueReturnValues.entrySet()) {
                context.getLogger().info("Subdialogue return value:"
                                         + returnValue.getKey()
                                         + "="
                                         + returnValue.getValue());
            }

        }

        //end of dialogue
        return new Exit("exit");
    }
}
