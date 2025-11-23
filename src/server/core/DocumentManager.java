package server.core;

import global.object.EditMessage;

public class DocumentManager {

    private StringBuilder document = new StringBuilder();

    public synchronized void apply(EditMessage msg) {

        switch (msg.mode) {
            case INSERT -> {
                document.insert(msg.offset, msg.text);
            }
            case DELETE -> {
                document.delete(msg.offset, msg.offset + msg.length);
            }

            //FULL_SYNC 추가 할 계획
            //새로 들어온 클라이언트에게 서버가 현재 문서 전체를 내려줄 때 사용 예정
        }
    }

    public synchronized String getDocument() {
        return document.toString();
    }
}
