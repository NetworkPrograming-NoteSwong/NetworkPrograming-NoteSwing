package server.core;

import global.enums.Mode;
import global.object.EditMessage;
import global.object.DocumentState;
import server.ui.ServerDashboardUI;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private final int port;
    private final ServerDashboardUI ui;
    private final DocumentManager doc = new DocumentManager();
    private final DocumentFileStore fileStore = new DocumentFileStore();

    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private final List<ClientHandler> handlers = new ArrayList<>();

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

                // 새 클라이언트에게 현재 문서 전체 + 이미지 동기화
                String current = doc.getDocument();
                if (!current.isEmpty()) {
                    EditMessage full = new EditMessage(Mode.FULL_SYNC, "server", current);
                    full.offset = 0;
                    full.length = current.length();
                    handler.send(full);

                    for (EditMessage imgMsg : doc.buildFullImageSyncMessages("server")) {
                        handler.send(imgMsg);
                    }
                }

                handler.start();
            }
        } catch (Exception e) {
            if (running) {
                ui.printDisplay("[서버 오류] " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        running = false;
        try {
            for (ClientHandler handler : handlers) {
                try {
                    handler.getClientSocket().close();
                } catch (IOException ignored) {}
            }
            handlers.clear();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            ui.printDisplay("서버 소켓 종료 오류: " + e.getMessage());
        }
    }

    public synchronized void broadcast(EditMessage msg, ClientHandler sender) {
        ui.printDisplay("[메시지 수신] " + msg);
        doc.apply(msg);
        for (ClientHandler handler : handlers) {
            if (handler == sender) continue;
            handler.send(msg);
        }
    }

    public synchronized void removeHandler(ClientHandler handler) {
        handlers.remove(handler);
    }

    // ==== 파일 입출력 ====
    public void saveDocumentToFile(String path) {
        try {
            DocumentState state = doc.createState();
            fileStore.save(state, new File(path));
            ui.printDisplay("[저장 완료] " + path);
        } catch (IOException e) {
            ui.printDisplay("[저장 실패] " + e.getMessage());
        }
    }

    public void loadDocumentFromFile(String path) {
        try {
            DocumentState state = fileStore.load(new File(path));
            doc.loadState(state);
            ui.printDisplay("[불러오기 완료] " + path);
        } catch (Exception e) {
            ui.printDisplay("[불러오기 실패] " + e.getMessage());
        }
    }
}
