/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.audio.*;

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

        //Play a synthesis message in another language
        Message message = new Message("synthesis-french-message",
                                      new SpeechSynthesis("Ceci est un message."));
        message.setLanguage("fr-CA");
        DialogueUtils.doTurn(message, context);

        //Alternatively, you can change the default VoiceXML language. This is done in the context.
        //This will change the language for all up coming message for which no language has been set. 
        context.setLanguage("fr-CA");

        Message otherMessage = new Message("synthesis-french-message",
                                           new SpeechSynthesis("Ceci est un autre message."));
        DialogueUtils.doTurn(otherMessage, context);

        Message yetAnotherMessage = new Message("synthesis-french-message",
                                                new SpeechSynthesis("Et encore un."));
        DialogueUtils.doTurn(yetAnotherMessage, context);

        //end of dialogue
        return new Exit("exit");
    }

}
