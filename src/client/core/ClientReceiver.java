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
                    case DOC_LIST -> controller.onRemoteDocList(msg.docs);
                    case FULL_SYNC -> controller.onRemoteFullSync(msg.docId, msg.docTitle, msg.text);
                    case SYNC_END -> controller.onRemoteSyncEnd(msg.docId);
                    case INSERT -> controller.onRemoteInsert(msg.docId, msg.offset, msg.text);
                    case DELETE -> controller.onRemoteDelete(msg.docId, msg.offset, msg.length);
                    case IMAGE_INSERT -> controller.onRemoteImageInsert(msg.docId, msg.blockId, msg.offset, msg.width, msg.height, msg.payload);
                    case IMAGE_RESIZE -> controller.onRemoteImageResize(msg.docId, msg.blockId, msg.width, msg.height);
                    case IMAGE_MOVE -> controller.onRemoteImageMove(msg.docId, msg.blockId, msg.newOffset);
                    case LOCK -> controller.onRemoteLock(msg.blockId, msg.userId);
                    case UNLOCK -> controller.onRemoteUnlock(msg.blockId, msg.userId);
                    case DOC_DELETED -> controller.onRemoteDocDeleted(msg.docId);
                    default -> { /* JOIN/LEAVE 등은 무시 */ }
                }
            }
        } catch (Exception e) {
            controller.onConnectionLost();
        }
    }
}
