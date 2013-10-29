/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import static com.nuecho.rivr.voicexml.turn.input.VoiceXmlEvent.*;

import java.util.*;

import javax.json.*;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.audio.*;
import com.nuecho.rivr.voicexml.util.*;
import com.nuecho.rivr.voicexml.util.json.*;

public class Dialogue implements VoiceXmlDialogue {

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context) {

        JsonObjectBuilder resultObjectBuilder = JsonUtils.createObjectBuilder();
        String status;

        try {
            // Play a prompt
            Message message = new Message("message", new SpeechSynthesis("Hello World!"));
            VoiceXmlInputTurn inputTurn = DialogueUtils.doTurn(message, context);

            // Handling hangup or error events
            List<VoiceXmlEvent> events = inputTurn.getEvents();
            if (events.isEmpty()) {
                status = "Normal";
            } else if (hasEvent(CONNECTION_DISCONNECT_HANGUP, events)) {
                status = "HangUp";
            } else if (hasEvent(ERROR, events)) {
                status = "PlatformError";
                VoiceXmlEvent errorEvent = getEvent(ERROR, events);
                resultObjectBuilder.add("eventName", errorEvent.getName());
                resultObjectBuilder.add("eventMessage", errorEvent.getMessage());
            } else {
                status = "Unknown";
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            status = "Interrupted";
        } catch (Throwable throwable) {
            status = "SystemError";
            context.getLogger().error("Error during dialogue execution", throwable);
            resultObjectBuilder.add("error", ResultUtils.toJson(throwable));
        }

        resultObjectBuilder.add("status", status);
        VariableList variables = VariableList.create(resultObjectBuilder.build());

        return new Exit("result", variables);
    }
}
