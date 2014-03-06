/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import java.util.*;
import java.util.Map.Entry;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.audio.*;

public class Dialogue implements VoiceXmlDialogue {

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context) throws Exception {

        Map<String, String> parameters = firstTurn.getParameters();

        List<AudioItem> audioItems = new ArrayList<AudioItem>();
        for (Entry<String, String> entry : parameters.entrySet()) {
            SpeechSynthesis speechSynthesis = new SpeechSynthesis("Parameter " + entry.getKey() + " has value " + entry.getValue() + ". ");
            audioItems.add(speechSynthesis);
        }

        Message outputTurn = new Message("message", audioItems);
        DialogueUtils.doTurn(outputTurn, context);

        //end of dialogue
        return new Exit("exit");
    }
}
