package global.config;

import java.io.BufferedReader;
import java.io.FileReader;

public class ConfigReader {

    public static ServerConfig load(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            String ip = br.readLine().trim();       // 첫 번째 줄: IP
            int port = Integer.parseInt(br.readLine().trim());  // 두 번째 줄: PORT

            return new ServerConfig(ip, port);

        } catch (Exception e) {
            System.out.println("[설정 오류] server.txt 읽기 실패: " + e.getMessage());
            return null;
        }
    }

    // 공통 DTO
    public static class ServerConfig {
        public final String ip;
        public final int port;

        public ServerConfig(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

}