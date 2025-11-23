package client.core;

import client.controller.EditorController;
import global.config.ConfigReader;
import global.object.EditMessage;

import java.io.*;
import java.net.Socket;

public class Client {

    private String serverIp;
    private int serverPort;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final EditorController controller;
    private volatile boolean connected = false;

    public Client(EditorController controller) {
        this.controller = controller;

        // server.txt 읽기
        ConfigReader.ServerConfig config = ConfigReader.load("server.txt");
        this.serverIp = config.ip;
        this.serverPort = config.port;
    }

    // 프로그램 시작 시 자동 연결 시도
    public void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket(serverIp, serverPort);

                // 객체 스트림 생성
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                controller.onConnectionStatus("서버 연결 완료");
                connected = true;

                // 메시지 수신 스레드 시작
                //현재는 서버가 우리한테 아무것도 안 보내니까, 수신 스레드는 잠깐 끈다.
                //new ClientReceiver(in, controller).start();

            } catch (Exception e) {
                connected = false;
                System.err.println("서버 연결 실패: " + e.getMessage());
                controller.onConnectionStatus("서버 연결 실패");
            }

        }).start();
    }


    // 서버로 메시지 보내기
    public void send(EditMessage msg) {
        if (!connected) return;

        try {
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            System.err.println("전송 오류: " + e.getMessage());
        }
    }

    // 종료
    public void disconnect() {
        try {
            connected = false;
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}
