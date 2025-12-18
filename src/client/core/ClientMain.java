package client.core;

import client.controller.EditorController;
import client.ui.LobbyUI;

public class ClientMain {
    public static void main(String[] args) {
        String userId = "user-" + System.currentTimeMillis();

        EditorController controller = new EditorController(userId);

        LobbyUI lobby = new LobbyUI(controller);
        controller.attachLobby(lobby);

        controller.start(); // 서버 연결
    }
}
