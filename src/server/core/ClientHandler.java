package server.core;

import global.object.EditMessage;
import server.ui.ServerDashboardUI;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {

    private final Socket clientSocket;
    private final Server server;
    private final ServerDashboardUI ui;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    private volatile String currentDocId = null;
    private String userId;

    public ClientHandler(Socket socket, Server server, ServerDashboardUI ui) {
        this.clientSocket = socket;
        this.server = server;
        this.ui = ui;

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
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

                if (msg.userId != null && userId == null) {
                    userId = msg.userId;
                }

                if (msg.docId != null) {
                    currentDocId = msg.docId;
                }

                server.handleFromClient(msg, this);
            }
        } catch (Exception e) {
            ui.printDisplay("[클라이언트 종료] 연결이 끊어졌습니다.");
            server.onClientDisconnected(this);
        }
    }

    public void send(EditMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            ui.printDisplay("[전송 오류] " + e.getMessage());
        }
    }

    public void close() {
        try { clientSocket.close(); } catch (Exception ignored) {}
    }

    public String getCurrentDocId() { return currentDocId; }

    public void setCurrentDocId(String docId) { this.currentDocId = docId; }

    public String getUserId() {
        return userId;
    }
}
