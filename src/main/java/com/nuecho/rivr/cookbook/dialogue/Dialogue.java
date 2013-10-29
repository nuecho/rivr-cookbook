/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import static com.nuecho.rivr.voicexml.turn.output.OutputTurns.*;

import javax.json.*;

import com.nuecho.rivr.core.channel.*;
import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.audio.*;
import com.nuecho.rivr.voicexml.turn.output.grammar.*;

public class Dialogue implements VoiceXmlDialogue {

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
            throws Exception {

        try {
            int number1 = askNumber(context);
            int number2 = askNumber(context);

            VariableList returnValues = new VariableList();
            returnValues.addWithExpression("number1", String.valueOf(number1));
            returnValues.addWithExpression("number2", String.valueOf(number2));
            returnValues.addWithExpression("sum", String.valueOf(number1 + number2));

            //return the values
            return new Return("return", returnValues);
        } catch (Exception exception) {
            //return an event
            return new Return("return", "rivr.cookbook.error", exception.getMessage());
        }
    }

    private int askNumber(VoiceXmlDialogueContext context) throws Timeout, InterruptedException, Exception {
        DtmfRecognition configuration = new DtmfRecognition(new GrammarReference("builtin:dtmf/number"));

        Interaction interaction = interaction("interaction")
                .addPrompt(configuration, new SpeechSynthesis("Enter a number."))
                .build(configuration, Duration.seconds(5));

        VoiceXmlInputTurn inputTurn = DialogueUtils.doTurn(interaction, context);

        if (inputTurn.getRecognitionInfo() == null) throw new Exception("Unexpected result");

        JsonArray recognitionResult = inputTurn.getRecognitionInfo().getRecognitionResult();
        String number = recognitionResult.getJsonObject(0).getString("interpretation");
        return Integer.parseInt(number);
    }
}
