/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import static com.nuecho.rivr.core.dialogue.DialogueUtils.*;
import static com.nuecho.rivr.voicexml.turn.input.VoiceXmlEvent.*;
import static com.nuecho.rivr.voicexml.turn.output.OutputTurns.*;
import static javax.json.Json.*;

import javax.json.*;

import com.nuecho.rivr.core.channel.*;
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

        playMessage(context, "welcome", "Welcome");

        String number = null;
        for (int tryCount = 0; tryCount < 3 && number == null; tryCount++) {
            VoiceXmlInputTurn inputTurn = askNumber(context);
            if (inputTurn.getRecognitionInfo() != null) {
                JsonArray recognitionResult = inputTurn.getRecognitionInfo().getRecognitionResult();
                number = recognitionResult.getJsonObject(0).getString("interpretation");
                playMessage(context, "feedback", "You have entered: " + number);
            } else if (hasEvent(VoiceXmlEvent.NO_INPUT, inputTurn.getEvents())) {
                playMessage(context, "no-input", "You haven't entered anything.");
            } else if (hasEvent(VoiceXmlEvent.NO_MATCH, inputTurn.getEvents())) {
                playMessage(context, "no-match", "This is not a number.");
            }
        }

        JsonObjectBuilder resultBuilder = createObjectBuilder();
        if (number == null) {
            resultBuilder.add("status", "error");
        } else {
            resultBuilder.add("status", "success");
            resultBuilder.add("number", number);
        }

        //end of dialogue
        return new Exit("exit", VariableList.create(resultBuilder.build()));
    }

    private VoiceXmlInputTurn askNumber(VoiceXmlDialogueContext context)
            throws Timeout, InterruptedException {
        GrammarItem dtmfGrammar = new GrammarReference("builtin:dtmf/digits");
        DtmfRecognition dtmfRecognition = new DtmfRecognition(dtmfGrammar);
        Interaction turn = interaction("ask-number")
                .addPrompt(new SpeechSynthesis("Type a number."))
                .build(dtmfRecognition, Duration.seconds(5));
        return doTurn(turn, context);
    }

    private void playMessage(VoiceXmlDialogueContext context, String name, String messageText)
            throws Timeout, InterruptedException {
        Message message = new Message(name, new SpeechSynthesis(messageText));
        doTurn(message, context);
    }

}
