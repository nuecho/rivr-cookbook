/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import static com.nuecho.rivr.voicexml.turn.input.TransferStatus.*;
import static com.nuecho.rivr.voicexml.turn.input.VoiceXmlEvent.*;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;

public class Dialogue implements VoiceXmlDialogue {

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
            throws Exception {

        //Consultation transfer
        ConsultationTransfer consultationTransfer = new ConsultationTransfer("consultation-transfer",
                                                                             "sip:test@example.com");
        consultationTransfer.setTransferAudio(context.getContextPath() + "/audio/test.wav");
        consultationTransfer.setConnectTimeout(Duration.seconds(30));
        VoiceXmlInputTurn inputTurn = DialogueUtils.doTurn(consultationTransfer, context);

        if (hasEvent(CONNECTION_DISCONNECT_TRANSFER, inputTurn.getEvents())) {
            context.getLogger().info("Transfer done");
        } else if (hasEvent(CONNECTION_DISCONNECT_HANGUP, inputTurn.getEvents())) {
            context.getLogger().info("User hung up during the transfer.");
        } else if (hasEvent(ERROR, inputTurn.getEvents())) {
            context.getLogger().info("Something went wrong with the transfer.");
        } else {
            TransferStatusInfo transferStatusInfo = inputTurn.getTransferResult();
            String statusCode = transferStatusInfo.getStatus().getStatusCode();

            if (statusCode.equals(NO_ANSWER)) {
                context.getLogger().info("No answer.");
            } else if (statusCode.equals(BUSY)) {
                context.getLogger().info("Busy.");
            } else if (statusCode.equals(NETWORK_BUSY)) {
                context.getLogger().info("Network Busy.");
            } else {
                context.getLogger().info("Other status: " + statusCode);
            }
        }

        //end of dialogue
        return new Exit("exit");
    }

}
