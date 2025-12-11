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
                    case INSERT ->
                            controller.onRemoteInsert(msg.offset, msg.text);
                    case DELETE ->
                            controller.onRemoteDelete(msg.offset, msg.length);
                    case FULL_SYNC ->
                            controller.onRemoteFullSync(msg.text);
                    case IMAGE_INSERT ->
                            controller.onRemoteImageInsert(
                                    msg.blockId, msg.offset,
                                    msg.width, msg.height, msg.payload);
                    case IMAGE_RESIZE ->
                            controller.onRemoteImageResize(
                                    msg.blockId, msg.width, msg.height);
                    case IMAGE_MOVE ->
                            controller.onRemoteImageMove(
                                    msg.blockId, msg.newOffset);
                    default -> { /* JOIN/LEAVE 등은 무시 */ }
                }
            }
        } catch (Exception e) {
            controller.onConnectionLost();
        }
    }
}
