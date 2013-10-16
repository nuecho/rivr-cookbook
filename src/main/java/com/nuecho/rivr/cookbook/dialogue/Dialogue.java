/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import org.springframework.beans.factory.*;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.audio.*;

public class Dialogue implements VoiceXmlDialogue, InitializingBean {

    private String mMessage;

    public void setMessage(String message) {
        mMessage = message;
    }

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
            throws Exception {

        Message message = new Message("message", new SpeechSynthesis(mMessage));
        DialogueUtils.doTurn(message, context);

        //end of dialogue
        return new Exit("exit");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (mMessage == null) throw new BeanInitializationException("Property 'message' is not set.");
    }
}
