package org.richfaces.ui.behavior;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import org.richfaces.javascript.JSFunction;
import org.richfaces.javascript.ScriptString;
import org.richfaces.javascript.ScriptStringBase;
import org.richfaces.javascript.Message;

import javax.faces.application.FacesMessage;
import java.io.IOException;
import java.util.Iterator;

public class MessageUpdateScript extends ScriptStringBase implements ScriptString {
    private static final Function<? super FacesMessage, Message> MESSAGES_TRANSFORMER = new Function<FacesMessage, Message>() {
        public Message apply(FacesMessage msg) {
            return new Message(msg);
        }
    };
    private final ImmutableList<Message> messages;
    private final String clientId;

    public MessageUpdateScript(String clientId, Iterator<FacesMessage> messages) {
        this.clientId = clientId;
        this.messages = ImmutableList.copyOf(Iterators.transform(messages, MESSAGES_TRANSFORMER));
    }

    public void appendScript(Appendable target) throws IOException {
        JSFunction resetMessages = new JSFunction("RichFaces.csv.clearMessage", clientId);
        resetMessages.appendScript(target);
        target.append(';');
        for (Message message : messages) {
            JSFunction sendMessage = new JSFunction("RichFaces.csv.sendMessage", clientId, message);
            sendMessage.appendScript(target);
            target.append(';');
        }
    }
}
