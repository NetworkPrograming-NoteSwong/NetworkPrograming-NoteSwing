// src/client/core/ClientReceiver.java
package client.core;

import client.controller.EditorController;
import global.enums.Mode;
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

                Mode mode = msg.mode;
                switch (mode) {
                    case INSERT -> controller.onRemoteInsert(msg.offset, msg.text);
                    case DELETE -> controller.onRemoteDelete(msg.offset, msg.length);
                    case FULL_SYNC -> controller.onRemoteFullSync(msg.text);
                    case CURSOR -> controller.onRemoteCursor(msg.userId, msg.offset, msg.length);
                    case IMAGE_INSERT ->
                            controller.onRemoteImageInsert(msg.blockId, msg.offset, msg.payload, msg.width, msg.height);
                    case IMAGE_RESIZE ->
                            controller.onRemoteImageResize(msg.blockId, msg.width, msg.height);
                    case JOIN, LEAVE -> {
                        // 아직 별도 처리 안 함
                    }
                }
            }
        } catch (Exception e) {
            controller.onConnectionLost();
        }
    }
}