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
                if (msg.docId != null) docService.open(msg.docId, sender);
            }

            case DOC_CREATE -> {
                DocumentMeta created = docService.create(msg.docTitle);
                broadcastDocListToAll();
                docService.open(created.id, sender);
            }

            case DOC_LEAVE -> {
                // 편집기 -> 로비 이동시, room 멤버십 정리
                docService.leave(sender);
            }

            case DOC_DELETE -> {
                if (msg.docId == null) return;

                String deletedId = msg.docId;

                // 1) 삭제 수행
                docService.delete(deletedId);

                // 2) 목록 갱신 브로드캐스트 (모든 클라이언트)
                broadcastDocListToAll();

                // 3) 삭제된 문서를 보고 있던 클라이언트들은 "다른 문서로 강제 이동"시키지 말고
                //    DOC_DELETED 이벤트만 보내고 room에서 빼기
                synchronized (handlers) {
                    for (ClientHandler h : handlers) {
                        if (deletedId.equals(h.getCurrentDocId())) {
                            docService.leave(h);

                            EditMessage del = new EditMessage(Mode.DOC_DELETED, "server", null);
                            del.docId = deletedId;
                            h.send(del);
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
        int lineIndex = msg.blockId;
        String userId = msg.userId;

        String owner = lockManager.tryLock(docId, lineIndex, userId);
        EditMessage resp = new EditMessage(Mode.LOCK, owner, null);
        resp.docId = docId;
        resp.blockId = lineIndex;
        resp.userId = owner;

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

        String userId = h.getUserId();
        lockManager.releaseAllByUser(userId);

        synchronized (handlers) { handlers.remove(h); }
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
            for (ClientHandler h : handlers) h.send(res);
        }
    }

    private void broadcastToDoc(String docId, EditMessage msg) {
        synchronized (handlers) {
            for (ClientHandler h : handlers) {
                if (docId != null && docId.equals(h.getCurrentDocId())) {
                    h.send(msg);
                }
            }
        }
    }
}
