package client.core;

import client.controller.EditorController;
import global.object.EditMessage;
import java.io.ObjectInputStream;

public class ClientReceiver extends Thread {

    private final ObjectInputStream in;
    private final EditorController controller;

    public ClientReceiver(ObjectInputStream in, EditorController controller) {
        this.in = in;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            while (true) {
                EditMessage msg = (EditMessage) in.readObject();

                switch (msg.mode) {
                    case INSERT -> controller.onRemoteInsert(msg.offset, msg.text);
                    case DELETE -> controller.onRemoteDelete(msg.offset, msg.length);
                    case FULL_SYNC -> controller.onRemoteFullSync(msg.text);
                    case CURSOR -> controller.onRemoteCursor(msg.userId, msg.offset, msg.length);
                    case LOCK -> controller.onRemoteLock(msg.blockId, msg.userId);
                    case UNLOCK -> controller.onRemoteUnlock(msg.blockId, msg.userId);
                }
            }
        } catch (Exception e) {
            controller.onConnectionLost();
        }
    }
}
