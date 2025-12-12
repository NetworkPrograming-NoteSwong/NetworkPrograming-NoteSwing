package server.storage;

import java.io.*;

public class DocumentFileStore {

    public void saveObject(Serializable obj, File file) {
        if (obj == null || file == null) return;

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(obj);
        } catch (IOException e) {
            throw new RuntimeException("saveObject failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T loadObject(File file, Class<T> type) {
        if (file == null || !file.exists()) return null;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Object o = in.readObject();
            if (o == null) return null;
            if (!type.isInstance(o)) return null;
            return (T) o;
        } catch (Exception e) {
            throw new RuntimeException("loadObject failed: " + e.getMessage(), e);
        }
    }
}
