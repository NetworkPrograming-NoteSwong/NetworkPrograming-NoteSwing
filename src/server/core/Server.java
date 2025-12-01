package server.core;

import global.enums.Mode;
import global.object.EditMessage;
import server.core.manager.DocumentManager;
import server.core.manager.LineLockManager;
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
    private DocumentManager documentManager = new DocumentManager();
    private LineLockManager lockManager = new LineLockManager();
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

                String current = documentManager.getDocument();
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

        switch (msg.mode) {
            case LOCK -> {
                int line = msg.blockId;
                String userId = msg.userId;

                boolean ok = lockManager.tryLock(line, userId);

                if (ok) {
                    // 잠금 성공: 모든 클라이언트에게 이 줄이 userId에게 잠겼다고 알림
                    for (ClientHandler handler : handlers) {
                        handler.send(msg);
                    }
                }
            }
            case UNLOCK -> {
                int line = msg.blockId;
                String userId = msg.userId;

                lockManager.unlock(line, userId);

                // 잠금 해제도 브로드캐스트
                for (ClientHandler handler : handlers) {
                    handler.send(msg);
                }
            }
            default -> {
                if (msg.mode != Mode.CURSOR) {
                    documentManager.apply(msg);
                }
                for (ClientHandler handler : handlers) {
                    if (handler == sender) continue;
                    handler.send(msg);
                }
            }
        }
    }

    public synchronized void removeHandler(ClientHandler handler) {
        handlers.remove(handler);
    }
}
