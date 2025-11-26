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

                String current = doc.getDocument();
                if (!current.isEmpty()) {
                    EditMessage full = new EditMessage(Mode.FULL_SYNC, "server", current);
                    ui.printDisplay("[FULL_SYNC 전송] 새 클라이언트에게 전체 문서 전송: " + full);
                    handler.send(full);
                }

                handler.start();
            }

        } catch (Exception e) {
            if(running) ui.printDisplay("[서버 오류] " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            running = false;
            for (ClientHandler handeler : handlers) {
                handeler.getClientSocket().close();
            }
            serverSocket.close();
        } catch (IOException e) {
            ui.printDisplay("서버 소켓 종료");
        }
    }

    public synchronized void broadcast(EditMessage msg, ClientHandler sender) {
        ui.printDisplay("[메시지 수신] " + msg);
        doc.apply(msg);

        if (msg.mode != Mode.CURSOR) {
            doc.apply(msg);    // 커서는 문서 상태 안 바꿈
        }

        for (ClientHandler handler : handlers) {
            if (handler == sender) continue;
            handler.send(msg);
        }
    }

    public synchronized void removeHandler(ClientHandler handler) {
        handlers.remove(handler);
    }
}
