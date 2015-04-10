/*
 * Copyright (c) 2015 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.turns;

import javax.json.*;

import org.w3c.dom.*;

import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.rendering.voicexml.*;
import com.nuecho.rivr.voicexml.turn.output.*;

/**
 * A sample turn adding client-side logging functionality to Rivr.
 * 
 * @author Nu Echo Inc.
 */
public class LogTurn extends VoiceXmlOutputTurn {

    private final String mStatement;
    private final String mLabel;

    public LogTurn(String name, String statement, String label) {
        super(name);
        mStatement = statement;
        mLabel = label;
    }

    @Override
    protected String getOuputTurnType() {
        return "log";
    }

    @Override
    protected void fillVoiceXmlDocument(Document document, Element formElement, VoiceXmlDialogueContext dialogueContext)
            throws VoiceXmlDocumentRenderingException {
        Element logElement = DomUtils.appendNewElement(formElement, VoiceXmlDomUtil.LOG_ELEMENT);
        if (mLabel != null) {
            logElement.setAttribute("label", mLabel);
        }

        if (mStatement != null) {
            DomUtils.appendNewText(logElement, mStatement);
        }

        VoiceXmlDomUtil.createGotoSubmit(formElement);
    }

    @Override
    protected void addTurnProperties(JsonObjectBuilder builder) {
        if (mStatement != null) {
            builder.add("statement", mStatement);
        }

        if (mLabel != null) {
            builder.add("label", mLabel);
        }
    }

}
