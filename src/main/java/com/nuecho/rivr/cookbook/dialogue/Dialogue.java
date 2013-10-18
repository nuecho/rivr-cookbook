/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import static com.nuecho.rivr.voicexml.turn.output.OutputTurns.*;

import java.io.*;

import org.slf4j.*;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.servlet.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.audio.*;

/**
 * The goal of the <code>Interaction</code> is to collect information from the
 * caller (via DTMF recognition, speech recognition or recording) by playing
 * <i>prompts</i> to the user.
 * <p>
 * Contrary to other VoiceXML OutputTurn classes, the <code>Interaction</code>
 * does not necessary map to a simple VoiceXML construct. As a result,
 * <code>Interaction</code> can be cumbersome to work with. Thus, it is
 * recommended to use the <code>Interaction.Builder</code> class to create
 * instances of <code>Interaction</code>.
 * <p>
 * Building an interaction boils down to:
 * <ol>
 * <li>add prompts, i.e. messages that will be played to the caller.
 * <p>
 * NOTE: Every of those prompts can be configured to do DTMF or speech
 * recognition (this is how to implement a <i>barge-in</i> behavior)</li>
 * <li>specify what to do after the prompts are played:
 * <ul>
 * <li>do nothing</li>
 * <li>perform recognition (DTMF, speech or both)</li>
 * <li>perform recording</li>
 * </ul>
 * </li>
 * </ol>
 * 
 * @see com.nuecho.rivr.voicexml.turn.output.Interaction.Builder
 * @see com.nuecho.rivr.voicexml.turn.output.Interaction
 * @author Nu Echo Inc.
 */
public class Dialogue implements VoiceXmlDialogue {

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
            throws Exception {

        Recording recording = new Recording();
        recording.setBeep(true);
        recording.setPostAudioToServer(true);
        recording.setType("audio/x-wav");
        recording.setFinalSilence(Duration.seconds(2));
        recording.setDtmfTerm(true);
        recording.setMaximumTime(Duration.seconds(20));

        SpeechSynthesis synthesisText = new SpeechSynthesis("Say your message after the beep.");
        Interaction interaction = interaction("record")
                .addPrompt(synthesisText)
                .build(recording, Duration.seconds(5));

        VoiceXmlInputTurn inputTurn = DialogueUtils.doTurn(interaction, context);

        Logger logger = context.getLogger();
        if (inputTurn.getRecordingInfo() != null) {
            RecordingInfo recordingInfo = inputTurn.getRecordingInfo();
            logger.info("DTMF term char: " + recordingInfo.getDtmfTerm());
            logger.info("Maximum time reached: " + recordingInfo.isMaxTime());
            logger.info("Recording duration: " + recordingInfo.getDuration());

            FileUpload file = recordingInfo.getFile();
            logger.info("Recording filename: " + file.getName());
            logger.info("Recording type: " + file.getContentType());
            logger.info("Recording size: " + file.getContent().length);

            File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"));
            File outputFile = new File(temporaryDirectory, "recording.wav");
            logger.info("Writing audio file to: " + outputFile.getAbsolutePath());
            OutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(file.getContent());
            outputStream.close();
        } else if (VoiceXmlEvent.hasEvent(VoiceXmlEvent.NO_INPUT, inputTurn.getEvents())) {
            logger.info("Timeout.");
        }

        //end of dialogue
        return new Exit("exit");
    }

}
