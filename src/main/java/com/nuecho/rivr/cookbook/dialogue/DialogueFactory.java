/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.dialogue;

import static org.springframework.web.context.support.WebApplicationContextUtils.*;

import org.springframework.web.context.*;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.core.servlet.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.output.*;

/**
 * @author Nu Echo Inc.
 */
public class DialogueFactory implements VoiceXmlDialogueFactory {

    @Override
    public VoiceXmlDialogue create(DialogueInitializationInfo<VoiceXmlInputTurn,
                                   VoiceXmlOutputTurn, VoiceXmlDialogueContext> initializationInfo)
            throws DialogueFactoryException {

        if (!(initializationInfo instanceof WebDialogueInitializationInfo))
            throw new DialogueFactoryException("Can only work in web mode.");

        WebDialogueInitializationInfo<?, ?, ?> webInitializationInfo =
                (WebDialogueInitializationInfo<?, ?, ?>) initializationInfo;
        WebApplicationContext applicationContext =
                getRequiredWebApplicationContext(webInitializationInfo.getServletContext());
        String dialogueBeanName = webInitializationInfo.getHttpServletRequest().getParameter("dialogue");

        if (dialogueBeanName == null)
            throw new DialogueFactoryException("Missing HTTP parameter 'dialogue'.");

        if (!applicationContext.containsBean(dialogueBeanName))
            throw new DialogueFactoryException("No such dialogue: '" + dialogueBeanName + "'");

        if (!applicationContext.isTypeMatch(dialogueBeanName, VoiceXmlDialogue.class))
            throw new DialogueFactoryException("Bad type for dialogue: '"
                                               + dialogueBeanName
                                               + "'.  Should be a "
                                               + VoiceXmlDialogue.class.getName());

        VoiceXmlDialogue dialogue = applicationContext.getBean(dialogueBeanName, VoiceXmlDialogue.class);
        if (dialogue == null)
            throw new DialogueFactoryException("No such dialogue: '" + dialogueBeanName + "'");

        return dialogue;
    }
}
