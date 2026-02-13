package com.antlab.rigcontrol;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ADBService {
    private final Settings settings;
    private final Logger logger;
    private final ExecutorService ioPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "adb-io");
        t.setDaemon(true);
        return t;
    });

    public ADBService(Settings settings, Logger logger) {
        this.settings = settings;
        this.logger = logger;
    }

    public ExecResult runAdb(List<String> args, Duration timeout) {
        List<String> command = new ArrayList<>();
        command.add(settings.getResolvedAdbPath());
        command.addAll(args);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);

        try {
            Process process = builder.start();
            Future<String> stdoutFuture = ioPool.submit(() -> readStream(process.getInputStream()));
            Future<String> stderrFuture = ioPool.submit(() -> readStream(process.getErrorStream()));

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ExecResult.timeout();
            }

            int exitCode = process.exitValue();
            String stdout = stdoutFuture.get(1, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(1, TimeUnit.SECONDS);
            return new ExecResult(exitCode, stdout, stderr, false);
        } catch (Exception e) {
            logger.log(Level.WARNING, "ADB command failed", e);
            return ExecResult.error(e.getMessage());
        }
    }

    private String readStream(InputStream stream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    public void shutdown() {
        ioPool.shutdownNow();
    }

    public static class ExecResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;
        public final boolean timedOut;

        public ExecResult(int exitCode, String stdout, String stderr, boolean timedOut) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.timedOut = timedOut;
        }

        public static ExecResult timeout() {
            return new ExecResult(-1, "", "timeout", true);
        }

        public static ExecResult error(String message) {
            return new ExecResult(-1, "", message == null ? "error" : message, false);
        }
    }
}
