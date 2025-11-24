// src/client/core/ClientMain.java
package client.core;

import client.controller.EditorController;
import client.ui.EditorMainUI;

public class ClientMain {

    public static void main(String[] args) {
        //로그인 하기 전이라 미리 생성
        String userId = "user-" + System.currentTimeMillis();

        // UI 먼저 생성
        EditorMainUI ui = new EditorMainUI();

        // Controller 생성 (UI 전달)
        EditorController controller = new EditorController(ui, userId);

        // UI에 Controller 주입
        ui.setController(controller);

        // 서버 연결 시작
        controller.start();
    }
}
