package server.core;

import global.config.ConfigReader;
import server.ui.ServerDashboardUI;

public class ServerMain {
    public static void main(String[] args) {

        // UI 생성
        ServerDashboardUI ui = new ServerDashboardUI();

        // server.txt 읽기
        ConfigReader.ServerConfig config = ConfigReader.load("server.txt");

        if (config == null) {
            ui.printDisplay("[오류] 설정 파일을 불러올 수 없습니다.");
            return;
        }

        Server server = new Server(config.port, ui);

        ui.setOnStartServer(() -> {
            Thread serverThread = new Thread(server::startServer);
            serverThread.start();
        });
        ui.setOnStopServer(() -> server.disconnect());

        ui.printDisplay("[서버 설정 로드 완료]");
        ui.printDisplay("IP : " + config.ip);
        ui.printDisplay("PORT : " + config.port);
    }
}