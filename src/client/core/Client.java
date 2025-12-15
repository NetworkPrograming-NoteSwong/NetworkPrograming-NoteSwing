package client.core;

import client.controller.EditorController;
import global.config.ConfigReader;
import global.object.EditMessage;

import java.io.*;
import java.net.Socket;

public class Client {

    private final String serverIp;
    private final int serverPort;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final EditorController controller;
    private volatile boolean connected = false;

    public Client(EditorController controller) {
        this.controller = controller;

        ConfigReader.ServerConfig config = ConfigReader.load("server.txt");
        this.serverIp = config.ip;
        this.serverPort = config.port;
    }

    public void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket(serverIp, serverPort);

                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                connected = true;
                controller.onConnectionStatus("서버 연결 완료");

                // 수신 스레드
                new ClientReceiver(in, controller).start();

                // 연결 완료 콜백(문서 목록 요청)
                controller.onConnected();

            } catch (Exception e) {
                connected = false;
                controller.onConnectionStatus("서버 연결 실패");
            }
        }).start();
    }

    public void send(EditMessage msg) {
        if (!connected || msg == null) return;
        try {
            out.writeObject(msg);
            out.flush();
        } catch (Exception ignored) {}
    }
}
