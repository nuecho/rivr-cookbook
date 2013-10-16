/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;

public class Dialogue implements VoiceXmlDialogue {

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
            throws Exception {

        //Bind transfer
        BlindTransfer blindTransfer = new BlindTransfer("blind-transfer", "sip:test@example.com");
        VoiceXmlInputTurn inputTurn = DialogueUtils.doTurn(blindTransfer, context);

        if (VoiceXmlEvent.hasEvent(VoiceXmlEvent.CONNECTION_DISCONNECT_TRANSFER, inputTurn.getEvents())) {
            context.getLogger().info("Transfer done");
        } else {
            context.getLogger().info("Something went wrong with the transfer.");
        }

        //end of dialogue
        return new Exit("exit");
    }

}
