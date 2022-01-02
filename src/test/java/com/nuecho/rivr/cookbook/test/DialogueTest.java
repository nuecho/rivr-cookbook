/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.test;

import static junit.framework.TestCase.*;

import java.util.*;

import org.junit.*;

import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.test.*;
import com.nuecho.rivr.voicexml.turn.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.Interaction.FinalRecognitionWindow;
import com.nuecho.rivr.voicexml.turn.output.Interaction.Prompt;
import com.nuecho.rivr.voicexml.turn.output.audio.*;
import com.nuecho.rivr.voicexml.turn.output.grammar.*;
import com.nuecho.rivr.voicexml.util.json.*;

import com.nuecho.rivr.cookbook.dialogue.*;

/**
 * @author Nu Echo Inc.
 */
public class DialogueTest {

    private final VoiceXmlTestDialogueChannel mChannel =
            new VoiceXmlTestDialogueChannel("test", Duration.seconds(15));

    private final VoiceXmlDialogueContext mDialogueContext =
            new VoiceXmlDialogueContext(mChannel,
                                        mChannel.getLogger(),
                                        "testDialogueId",
                                        "/context",
                                        "/servlet");

    @Test
    public void testNormal() {
        startDialogue();
        assertWelcomeMessage();

        mChannel.processNoAction();
        assertPromptForNumber();

        mChannel.processDtmfRecognition("1 2 3 4 5 6", JsonUtils.wrap("123456"), null);
        assertMessage(new SpeechSynthesis("You have entered: 123456"));

        mChannel.processNoAction();
        VariableList expectedVariables = new VariableList();
        expectedVariables.addWithExpression("status", "\"success\"");
        expectedVariables.addWithExpression("number", "\"123456\"");
        assertExit(expectedVariables);
    }

    @Test
    public void testNoInput() {
        startDialogue();
        assertWelcomeMessage();

        mChannel.processNoAction();
        assertPromptForNumber();

        mChannel.processNoInput();
        assertNoInputMessage();

        mChannel.processNoAction();
        assertPromptForNumber();

        mChannel.processNoInput();
        assertNoInputMessage();

        mChannel.processNoAction();
        assertPromptForNumber();

        mChannel.processNoInput();
        assertNoInputMessage();

        mChannel.processNoAction();
        VariableList expectedVariables = new VariableList();
        expectedVariables.addWithExpression("status", "\"error\"");
        assertExit(expectedVariables);
    }

    @Test
    public void testNoMatch() {
        startDialogue();
        assertWelcomeMessage();

        mChannel.processNoAction();
        assertPromptForNumber();

        mChannel.processNoMatch();
        assertNoMatchMessage();

        mChannel.processNoAction();
        assertPromptForNumber();

        mChannel.processNoMatch();
        assertNoMatchMessage();

        mChannel.processNoAction();
        assertPromptForNumber();

        mChannel.processNoMatch();
        assertNoMatchMessage();

        mChannel.processNoAction();
        VariableList expectedVariables = new VariableList();
        expectedVariables.addWithExpression("status", "\"error\"");
        assertExit(expectedVariables);
    }

    @Test
    public void testOtherEvent() {
        startDialogue();
        assertWelcomeMessage();

        mChannel.processNoAction();
        assertPromptForNumber();

        mChannel.processEvent("unexpectedevent");
        assertPromptForNumber();

        mChannel.processEvent("unexpectedevent");
        assertPromptForNumber();

        mChannel.processEvent("unexpectedevent");
        VariableList expectedVariables = new VariableList();
        expectedVariables.addWithExpression("status", "\"error\"");
        assertExit(expectedVariables);
    }

    private void startDialogue() {
        mChannel.startDialogue(new Dialogue(), new VoiceXmlFirstTurn(), mDialogueContext);
    }

    private void assertWelcomeMessage() {
        assertMessage(new SpeechSynthesis("Welcome"));
    }

    private void assertPromptForNumber() {
        assertDtmfInteraction(Duration.seconds(5),
                              "builtin:dtmf/digits",
                              new SpeechSynthesis("Type a number."));
    }

    private void assertNoInputMessage() {
        assertMessage(new SpeechSynthesis("You haven't entered anything."));
    }

    private void assertNoMatchMessage() {
        assertMessage(new SpeechSynthesis("This is not a number."));
    }

    private void assertMessage(AudioItem... expectedAudioItems) {
        VoiceXmlOutputTurn turn = mChannel.getLastStepAsOutputTurn();
        assertEquals(Message.class, turn.getClass());
        Message actualTurn = (Message) turn;
        assertEquals(Arrays.asList(expectedAudioItems), actualTurn.getAudioItems());
    }

    private void assertDtmfInteraction(Duration expectedTimeout,
                                       String expectedGrammar,
                                       AudioItem... expectedAudioItems) {
        DtmfRecognition dtmfRecognition = new DtmfRecognition(new GrammarReference(expectedGrammar));
        assertInteraction(new FinalRecognitionWindow(dtmfRecognition, expectedTimeout),
                          new Prompt(expectedAudioItems));
    }

    private void assertInteraction(FinalRecognitionWindow expectedRecognition, Prompt... expectedPrompts) {
        VoiceXmlOutputTurn turn = mChannel.getLastStepAsOutputTurn();
        assertEquals(Interaction.class, turn.getClass());
        Interaction actualTurn = (Interaction) turn;
        assertEquals(Arrays.asList(expectedPrompts), actualTurn.getPrompts());
        assertEquals(expectedRecognition, actualTurn.getRecognition());
    }

    private void assertExit(VariableList expectedVariables) {
        VoiceXmlLastTurn turn = mChannel.getLastStepAsLastTurn();
        assertEquals(Exit.class, turn.getClass());
        Exit exitTurn = (Exit) turn;
        VariableList actualVariables = exitTurn.getVariables();
        assertEquals(expectedVariables, actualVariables);
    }
}
