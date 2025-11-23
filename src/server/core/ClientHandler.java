package server.core;

import global.object.EditMessage;
import server.ui.ServerDashboardUI;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {

    private Socket clientSocket;
    private Server server;
    private ServerDashboardUI ui;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket, Server server, ServerDashboardUI ui) {
        this.clientSocket = socket;
        this.server = server;
        this.ui = ui;

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // 중요: 먼저 flush해야 OIS deadlock 방지
            in = new ObjectInputStream(socket.getInputStream());

        } catch (Exception e) {
            ui.printDisplay("[핸들러 오류] 스트림 생성 실패: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                EditMessage msg = (EditMessage) in.readObject();
                server.broadcast(msg, this);
            }//recieve 메서드 분리 할 계획

        } catch (Exception e) {
            ui.printDisplay("[클라이언트 종료] 클라이언트 연결이 끊어졌습니다.");
            System.err.println("연결 종료: " + e.getMessage());
            server.removeHandler(this);
        }
    }

    public void send(EditMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();

        } catch (Exception e) {
            ui.printDisplay("[전송 오류] 클라이언트로 메시지를 보내는 중 오류 발생: " + e.getMessage());
        }
    }

    public void close() {
        try {
            clientSocket.close();
        } catch (Exception ignored) {}
    }

    public Socket getClientSocket() {
        return clientSocket;
    }
}
