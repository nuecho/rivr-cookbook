/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import javax.json.*;

import com.nuecho.rivr.core.channel.*;
import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.rendering.voicexml.*;
import com.nuecho.rivr.voicexml.turn.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.audio.*;
import com.nuecho.rivr.voicexml.turn.output.grammar.*;
import com.nuecho.rivr.voicexml.turn.output.interaction.*;

public class Dialogue implements VoiceXmlDialogue {

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
            throws Exception {

        try
        {
            int number1 = askNumber(context);
            int number2 = askNumber(context);

            VariableDeclarationList returnValues = new VariableDeclarationList();
            returnValues.addVariable(new VariableDeclaration("number1", String.valueOf(number1)));
            returnValues.addVariable(new VariableDeclaration("number2", String.valueOf(number2)));
            returnValues.addVariable(new VariableDeclaration("sum", String.valueOf(number1 + number2)));

            //return the values
            return new VoiceXmlReturnTurn("return", returnValues);
        } catch (Exception exception) {
            //return an event
            return new VoiceXmlReturnTurn("return", "rivr.cookbook.error", exception.getMessage());
        }
    }

    private int askNumber(VoiceXmlDialogueContext context) throws Timeout, InterruptedException, Exception {
        DtmfRecognitionConfiguration configuration =
                new DtmfRecognitionConfiguration(new GrammarReference("builtin:dtmf/number"));

        InteractionBuilder builder = InteractionBuilder.newInteractionBuilder("interaction");
        InteractionTurn interaction = builder.addPrompt(configuration, new SynthesisText("Enter a number."))
                                             .build(configuration, TimeValue.seconds(5));

        VoiceXmlInputTurn inputTurn = DialogueUtils.doTurn(context, interaction);

        if (inputTurn.getRecognitionInfo() == null) throw new Exception("Unexpected result");

        JsonArray recognitionResult = inputTurn.getRecognitionInfo().getRecognitionResult();
        String number = recognitionResult.getJsonObject(0).getString("interpretation");
        return Integer.parseInt(number);
    }
}
