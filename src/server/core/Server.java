package server.core;

import client.ui.LineLockManager;
import global.enums.Mode;
import global.object.DocumentMeta;
import global.object.EditMessage;
import server.document.DocumentService;
import server.storage.DocumentStorage;
import server.ui.ServerDashboardUI;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private final int port;
    private final ServerDashboardUI ui;

    private final DocumentStorage storage = new DocumentStorage();
    private final DocumentService docService = new DocumentService(storage);

    private volatile boolean running = false;
    private ServerSocket serverSocket;

    private final List<ClientHandler> handlers = new ArrayList<>();
    private final LineLockManager lockManager = new LineLockManager();

    public Server(int port, ServerDashboardUI ui) {
        this.port = port;
        this.ui = ui;
    }

    public void startServer() {
        try {
            running = true;
            serverSocket = new ServerSocket(port);
            ui.printDisplay("[SERVER] 서버 시작 (port=" + port + ")");

            while (running) {
                Socket socket = serverSocket.accept();
                ui.printDisplay("[SERVER] 클라이언트 연결");

                ClientHandler handler = new ClientHandler(socket, this, ui);
                synchronized (handlers) { handlers.add(handler); }

                sendDocListTo(handler);
                handler.start();
            }
        } catch (Exception e) {
            if (running) ui.printDisplay("[서버 오류] " + e.getMessage());
        }
    }

    public void disconnect() {
        running = false;
        try {
            synchronized (handlers) {
                for (ClientHandler h : handlers) {
                    try { h.close(); } catch (Exception ignored) {}
                }
                handlers.clear();
            }
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {
            ui.printDisplay("[서버 종료 오류] " + e.getMessage());
        }
    }

    public void handleFromClient(EditMessage msg, ClientHandler sender) {
        if (msg == null || msg.mode == null) return;

        ui.printDisplay("[FROM CLIENT] " + msg);

        switch (msg.mode) {
            case LOCK -> handleLock(msg);
            case UNLOCK -> handleUnlock(msg);
            case DOC_LIST -> sendDocListTo(sender);
            case DOC_OPEN -> {
                if (msg.docId != null) {
                    docService.open(msg.docId, sender);
                }
            }

            case DOC_CREATE -> {
                DocumentMeta created = docService.create(msg.docTitle);
                broadcastDocListToAll();
                docService.open(created.id, sender);
            }

            case DOC_DELETE -> {
                if (msg.docId == null) return;

                String deletedId = msg.docId;
                docService.delete(deletedId);
                broadcastDocListToAll();

                // 삭제된 문서를 보고 있던 사람들은 “첫 문서”로 자동 이동
                List<DocumentMeta> list = docService.listDocs();
                String fallback = (list.isEmpty() ? null : list.get(0).id);
                if (fallback == null) {
                    DocumentMeta created = docService.create("Untitled Document");
                    fallback = created.id;
                    broadcastDocListToAll();
                }

                synchronized (handlers) {
                    for (ClientHandler h : handlers) {
                        if (deletedId.equals(h.getCurrentDocId())) {
                            docService.open(fallback, h);
                        }
                    }
                }
            }

            case INSERT, DELETE, IMAGE_INSERT, IMAGE_RESIZE, IMAGE_MOVE -> {
                docService.applyEdit(msg, sender);
            }
            default -> {}
        }
    }

    private void handleLock(EditMessage msg) {
        String docId = msg.docId;
        int lineIndex = msg.blockId; // lineIndex로 사용
        String userId = msg.userId;

        String owner = lockManager.tryLock(docId, lineIndex, userId);
        EditMessage resp = new EditMessage(Mode.LOCK, owner, null);
        resp.docId = docId;
        resp.blockId = lineIndex;
        resp.userId = owner; // 해당 라인의 락 소유자

        // 같은 문서 편집 중인 모든 클라이언트에게 브로드캐스트
        broadcastToDoc(docId, resp);
    }

    private void handleUnlock(EditMessage msg) {
        String docId = msg.docId;
        int lineIndex = msg.blockId;
        String userId = msg.userId;

        lockManager.unlock(docId, lineIndex, userId);

        EditMessage resp = new EditMessage(Mode.UNLOCK, userId, null);
        resp.docId = docId;
        resp.blockId = lineIndex;
        resp.userId = userId;

        broadcastToDoc(docId, resp);
    }

    public void onClientDisconnected(ClientHandler h) {
        try { h.close(); } catch (Exception ignored) {}
        docService.leave(h);

        synchronized (handlers) {
            handlers.remove(h);
        }
    }

    private void sendDocListTo(ClientHandler h) {
        EditMessage res = new EditMessage(Mode.DOC_LIST, "server", null);
        res.docs = docService.listDocs();
        h.send(res);
    }

    private void broadcastDocListToAll() {
        EditMessage res = new EditMessage(Mode.DOC_LIST, "server", null);
        res.docs = docService.listDocs();

        synchronized (handlers) {
            for (ClientHandler h : handlers) {
                h.send(res);
            }
        }
    }

    private void broadcastToDoc(String docId, EditMessage msg) {
        synchronized (handlers) {
            for (ClientHandler h : handlers) {
                // 그 클라이언트가 지금 보고 있는 문서가 docId인 경우에만 전송
                if (docId.equals(h.getCurrentDocId())) {
                    h.send(msg);
                }
            }
        }
    }

}
