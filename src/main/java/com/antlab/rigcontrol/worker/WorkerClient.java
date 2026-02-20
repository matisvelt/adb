package com.antlab.rigcontrol.worker;

import com.antlab.rigcontrol.ADBService;
import com.antlab.rigcontrol.DeviceInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

public class WorkerClient {
    private final ADBService adbService;
    private final Logger logger;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public WorkerClient(ADBService adbService, Logger logger) {
        this.adbService = adbService;
        this.logger = logger;
    }

    public WorkerHealth checkHealth(DeviceInfo device) {
        WorkerHealth health = new WorkerHealth();
        if (device == null) {
            health.setOk(false);
            health.setError("Missing device");
            return health;
        }
        try {
            int port = ensureForward(device);
            URI uri = URI.create("http://127.0.0.1:" + port + "/health");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                health.setOk(false);
                health.setError("HTTP " + response.statusCode());
                return health;
            }
            return mapper.readValue(response.body(), WorkerHealth.class);
        } catch (Exception ex) {
            health.setOk(false);
            health.setError(ex.getMessage());
            return health;
        }
    }

    public List<WorkerResult> classifyBatch(DeviceInfo device, List<PreviewPayload> items) throws Exception {
        int port = ensureForward(device);
        URI uri = URI.create("http://127.0.0.1:" + port + "/classifyBatch");
        BatchRequest req = new BatchRequest();
        req.items = new ArrayList<>();
        for (PreviewPayload payload : items) {
            BatchItem item = new BatchItem();
            item.fileId = payload.fileId;
            item.ext = payload.ext;
            item.previewBase64 = Base64.getEncoder().encodeToString(payload.bytes);
            req.items.add(item);
        }
        String json = mapper.writeValueAsString(req);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Worker HTTP " + response.statusCode());
        }
        BatchResponse resp = mapper.readValue(response.body(), BatchResponse.class);
        return resp == null ? List.of() : resp.results;
    }

    private int ensureForward(DeviceInfo device) {
        int port = device.getForwardedPort();
        if (port <= 0) {
            port = PortAllocator.portForSerial(device.getSerial());
            int finalPort = port;
            Platform.runLater(() -> device.setForwardedPort(finalPort));
        }
        adbService.runAdb(List.of("-s", device.getSerial(), "forward", "tcp:" + port, "tcp:18080"), Duration.ofSeconds(3));
        return port;
    }

    public static class PreviewPayload {
        public final String fileId;
        public final byte[] bytes;
        public final String ext;

        public PreviewPayload(String fileId, byte[] bytes, String ext) {
            this.fileId = fileId;
            this.bytes = bytes;
            this.ext = ext;
        }

        public static PreviewPayload fromPath(String fileId, Path path) throws Exception {
            byte[] bytes = Files.readAllBytes(path);
            String ext = "";
            String name = path.getFileName().toString();
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                ext = name.substring(dot + 1);
            }
            return new PreviewPayload(fileId, bytes, ext);
        }
    }

    private static class BatchRequest {
        public List<BatchItem> items;
    }

    private static class BatchItem {
        public String fileId;
        public String previewBase64;
        public String ext;
    }

    private static class BatchResponse {
        public List<WorkerResult> results;
    }
}
