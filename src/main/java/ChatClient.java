import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.nio.file.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;

public class ChatClient extends WebSocketClient {
    private String token = null;
    private String serverIp = "192.168.1.6";

    public ChatClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("========================================");
        System.out.println("Conectado ao servidor!");
        System.out.println("Digite suas mensagens abaixo:");
        System.out.println("Comandos:");
        System.out.println("  /upload <arquivo>  - Enviar arquivo");
        System.out.println("  /download <arquivo> - Baixar arquivo");
        System.out.println("========================================");
    }

    @Override
    public void onMessage(String message) {
        if (message.startsWith("[SYSTEM] Seu token:")) {
            token = message.replace("[SYSTEM] Seu token: ", "").trim();
            System.out.println("\n" + message);
        } else {
            System.out.println("\n" + message);
        }
        System.out.print("Você: ");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Desconectado do servidor");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[ERRO] " + ex.getMessage());
        ex.printStackTrace();
    }

    public void uploadFile(String filename) {
        if (token == null) {
            System.out.println("Aguarde o token ser carregado...");
            return;
        }

        try {
            Path filepath = Paths.get(filename);
            if (!Files.exists(filepath)) {
                System.out.println("Arquivo não encontrado: " + filename);
                return;
            }

            byte[] fileBytes = Files.readAllBytes(filepath);

            URL url = new URL("http://" + serverIp + ":8081/upload");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", token);
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(fileBytes.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(fileBytes);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("Arquivo enviado com sucesso!");
            } else {
                System.out.println("Erro ao enviar arquivo. Código: " + responseCode);
            }

        } catch (Exception e) {
            System.err.println("Erro ao fazer upload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void downloadFile(String filename) {
        if (token == null) {
            System.out.println("Aguarde o token ser carregado...");
            return;
        }

        try {
            URL url = new URL("http://" + serverIp + ":8081/download?token=" + token + "&file=" + filename);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Path savePath = Paths.get("download_" + filename);
                try (InputStream is = conn.getInputStream()) {
                    Files.copy(is, savePath, StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("Arquivo baixado: " + savePath.toString());
            } else {
                System.out.println("Erro ao baixar arquivo. Código: " + responseCode);
            }

        } catch (Exception e) {
            System.err.println("Erro ao fazer download: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        String serverIp = "192.168.1.13"; // MUDA AQUI pro IP do teu Notebook
        String wsUrl = "ws://" + serverIp + ":8080";

        System.out.println("Tentando conectar em: " + wsUrl);

        ChatClient client = new ChatClient(new URI(wsUrl));

        if (!client.connectBlocking()) {
            System.err.println("Falha ao conectar!");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Você: ");

        while (true) {
            String input = scanner.nextLine();

            if (input.equals("sair")) {
                client.close();
                break;
            } else if (input.startsWith("/upload ")) {
                String filename = input.substring(8);
                client.uploadFile(filename);
            } else if (input.startsWith("/download ")) {
                String filename = input.substring(10);
                client.downloadFile(filename);
            } else {
                client.send(input);
            }
        }
    }
}