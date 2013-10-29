/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import static com.nuecho.rivr.voicexml.turn.output.OutputTurns.*;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.audio.*;
import com.nuecho.rivr.voicexml.turn.output.grammar.*;

/**
 * A message turn is a primitive to <i>queue</i> a message on the VoiceXML
 * platform. The message is not played immediately, but rather when the VoiceXML
 * platform decides to do so.
 * 
 * @see <a href="http://www.w3.org/TR/voicexml20/#dml4.1.8">VoiceXML
 *      specification, 4.1.8 Prompt Queueing and Input Collection</a>
 * @author Nu Echo Inc.
 */
public class Dialogue implements VoiceXmlDialogue {

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
            throws Exception {

        //Sets the fetch-audio to empty. This overrides some platform default settings.  
        // The idea is that we do not want fetch audio set because it would prevent the message to be queued.
        context.getFetchConfiguration().getDocumentFetchConfiguration().setFetchAudio("");

        String messageText = "This is a barge-in message that can be interrupted.";
        Message message = new Message("barge-in-message", new SpeechSynthesis(messageText));
        //Controls the barge-in flag:
        //   If barge-in is set to true, this message may be interrupted with DTMF or speech input 
        //     if this turn is followed by an interaction.
        //   If set to false, the prompt cannot be interrupted.
        message.setBargeIn(true);
        DialogueUtils.doTurn(message, context);

        DtmfRecognition configuration = new DtmfRecognition(new GrammarReference("builtin:dtmf/digits"));
        Interaction interaction = interaction("interaction")
                .addPrompt(configuration, new SpeechSynthesis("Enter a digit."))
                .build(configuration, Duration.seconds(5));
        DialogueUtils.doTurn(interaction, context);

        //end of dialogue
        return new Exit("exit");
    }
}
