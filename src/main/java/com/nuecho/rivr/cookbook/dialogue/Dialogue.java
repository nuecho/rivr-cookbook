/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.last.*;

import com.nuecho.rivr.cookbook.turns.*;

/**
 * This example shows how to create a custom VoiceXML output turn.
 * 
 * @author Nu Echo Inc.
 */
public class Dialogue implements VoiceXmlDialogue {

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
            throws Exception {

        LogTurn log = new LogTurn("logging", "this is a logging statement", "debug");
        DialogueUtils.doTurn(log, context);

        //end of dialogue
        return new Exit("exit");
    }
}
