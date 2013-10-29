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

        //Play an audio file with alternate content
        String recordingLocation = context.getContextPath() + "/audio/test.wav";
        String alternateText = "This message is played because the audio file could not be fetched";
        AudioFile audioFile = AudioFile.fromLocation(recordingLocation, alternateText);
        Message message = new Message("recording-message", audioFile);
        DialogueUtils.doTurn(message, context);

        //end of dialogue
        return new Exit("exit");
    }
}
