package server.core;

import global.object.EditMessage;

public class DocumentManager {

    private StringBuilder document = new StringBuilder();

    public synchronized void apply(EditMessage msg) {

        switch (msg.mode) {
            case INSERT -> {
                document.insert(msg.offset, msg.text);
            }
            case DELETE -> {
                document.delete(msg.offset, msg.offset + msg.length);
            }

            case FULL_SYNC -> {
                document.setLength(0);
                if (msg.text != null) {
                    document.append(msg.text);
                }
            }
        }
    }

    public synchronized String getDocument() {
        return document.toString();
    }
}
