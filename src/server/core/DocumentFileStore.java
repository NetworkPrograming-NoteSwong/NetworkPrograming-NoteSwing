package server.core;

import global.object.DocumentState;

import java.io.*;

public class DocumentFileStore {

    public void save(DocumentState state, File file) throws IOException {
        if (state == null || file == null) return;
        try (ObjectOutputStream out =
                     new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(state);
        }
    }

    public DocumentState load(File file)
            throws IOException, ClassNotFoundException {
        if (file == null || !file.exists()) return null;
        try (ObjectInputStream in =
                     new ObjectInputStream(new FileInputStream(file))) {
            return (DocumentState) in.readObject();
        }
    }
}
