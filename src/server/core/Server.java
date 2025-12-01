// src/server/core/Server.java
package server.core;

import global.enums.Mode;
import global.object.EditMessage;
import server.ui.ServerDashboardUI;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private int port;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private ServerDashboardUI ui;
    private DocumentManager doc = new DocumentManager();
    private List<ClientHandler> handlers = new ArrayList<>();

    public Server(int port, ServerDashboardUI ui) {
        this.port = port;
        this.ui = ui;
    }

    public void startServer() {
        try {
            running = true;
            serverSocket = new ServerSocket(port);
            ui.printDisplay("[SERVER] 서버가 시작되었습니다. (port=" + port + ")");

            while (running) {
                Socket socket = serverSocket.accept();
                ui.printDisplay("[SERVER] 클라이언트가 연결되었습니다.");

                ClientHandler handler = new ClientHandler(socket, this, ui);
                handlers.add(handler);

                // 새 클라이언트에게 현재 문서 전체(FULL_SYNC) 전송
                String current = doc.getDocument();
                if (!current.isEmpty()) {
                    EditMessage full = new EditMessage(Mode.FULL_SYNC, "server", current);
                    ui.printDisplay("[FULL_SYNC 전송] 새 클라이언트에게 전체 문서 전송: " + full);
                    handler.send(full);
                }

                // 이어서, 서버가 알고 있는 모든 이미지도 순서대로 전송
                for (DocumentManager.ImageState img : doc.getImages()) {
                    EditMessage imgMsg = new EditMessage(Mode.IMAGE_INSERT, "server", null);
                    imgMsg.blockId = img.blockId;
                    imgMsg.offset = img.offset;
                    imgMsg.payload = img.data;
                    imgMsg.width = img.width;
                    imgMsg.height = img.height;

                    ui.printDisplay("[IMAGE_SYNC 전송] 새 클라이언트에게 이미지 전송: " + imgMsg);
                    handler.send(imgMsg);
                }

                handler.start();
            }

        } catch (Exception e) {
            if (running) ui.printDisplay("[서버 오류] " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            running = false;
            for (ClientHandler handler : handlers) {
                handler.getClientSocket().close();
            }
            serverSocket.close();
        } catch (IOException e) {
            ui.printDisplay("서버 소켓 종료");
        }
    }

    public synchronized void broadcast(EditMessage msg, ClientHandler sender) {
        ui.printDisplay("[메시지 수신] " + msg);

        // CURSOR / JOIN / LEAVE 를 제외한 나머지는 문서 상태에 반영
        if (msg.mode != Mode.CURSOR && msg.mode != Mode.JOIN && msg.mode != Mode.LEAVE) {
            doc.apply(msg);
        }

        // 나를 제외한 다른 클라이언트에게만 전송
        for (ClientHandler handler : handlers) {
            if (handler == sender) continue;
            handler.send(msg);
        }
    }

    public synchronized void removeHandler(ClientHandler handler) {
        handlers.remove(handler);
    }
}