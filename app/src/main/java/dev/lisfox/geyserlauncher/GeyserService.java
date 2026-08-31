package dev.lisfox.geyserlauncher;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.net.wifi.WifiManager;
import android.system.Os;
import android.system.OsConstants;

import dev.lisfox.geyserlauncher.runtime.GeyserRuntime;
import dev.lisfox.geyserlauncher.runtime.JvmArguments;
import dev.lisfox.geyserlauncher.data.GeyserDistribution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GeyserService extends Service {
    public static final String START = "dev.lisfox.geyserlauncher.START";
    public static final String STOP = "dev.lisfox.geyserlauncher.STOP";
    public static final String FORCE_STOP = "dev.lisfox.geyserlauncher.FORCE_STOP";
    public static final String COMMAND = "dev.lisfox.geyserlauncher.COMMAND";
    public static final String EXTRA_COMMAND = "command";
    private static final String CHANNEL = "geyser";
    private static volatile Process process;
    private static volatile boolean stopRequested;
    private static volatile boolean starting;
    private final ExecutorService io = Executors.newCachedThreadPool();
    private OutputStreamWriter processInput;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private static void emit(String line, boolean running) { GeyserRuntime.emit(line, running); }

    public static boolean isProcessActive() {
        Process current = process;
        return starting || (current != null && current.isAlive());
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent != null && COMMAND.equals(intent.getAction())) sendCommand(intent.getStringExtra(EXTRA_COMMAND));
        else if (intent != null && FORCE_STOP.equals(intent.getAction())) forceStopGeyser();
        else if (intent != null && STOP.equals(intent.getAction())) stopGeyser();
        else if (intent != null && START.equals(intent.getAction())) {
            startForeground(7, notification("正在启动 Geyser"));
            acquireBackgroundLocks();
            startGeyser();
        } else {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private synchronized void startGeyser() {
        if (process != null && process.isAlive()) { emit("Geyser 已在运行", true); return; }
        if (starting) { emit("Geyser 正在启动", true); return; }
        starting = true;
        stopRequested = false;
        GeyserRuntime.beginStart();
        io.execute(() -> {
            try {
                File root = new File(getFilesDir(), "geyser");
                File jre = new File(root, "jre25");
                root.mkdirs();
                if (!new File(jre, "bin/java").exists()) {
                    emit("正在释放 JRE 25（首次启动较慢）...", false);
                    unzipAsset("jre25.zip", jre);
                    new File(jre, "bin/java").setExecutable(true);
                    emit("JRE 25 释放完成", false);
                }
                ensureJreExecutables(jre);
                File jar = GeyserDistribution.ensureInstalled(this, message -> emit(message, false));
                File work = new File(root, "server"); work.mkdirs();
                File java = new File(jre, "bin/java");
                File pidFile = new File(root, "geyser.pid");
                if (pidFile.exists()) pidFile.delete();
                File tmp = new File(root, "tmp");
                tmp.mkdirs();
                StringBuilder command = new StringBuilder("echo $$ > \"$GEYSER_PID_FILE\"; exec ")
                        .append(JvmArguments.shellQuote(java.getAbsolutePath()));
                for (String argument : JvmArguments.REQUIRED) {
                    command.append(' ').append(JvmArguments.shellQuote(argument));
                }
                command.append(' ').append(JvmArguments.shellQuote("-Djava.io.tmpdir=" + tmp.getAbsolutePath()));
                command.append(' ').append(JvmArguments.shellQuote("-Djna.tmpdir=" + tmp.getAbsolutePath()));
                for (String argument : JvmArguments.load(this)) {
                    command.append(' ').append(JvmArguments.shellQuote(argument));
                }
                command.append(' ').append(JvmArguments.shellQuote("-jar"));
                command.append(' ').append(JvmArguments.shellQuote(jar.getAbsolutePath()));
                ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", "-c", command.toString());
                pb.directory(work);
                Map<String, String> env = pb.environment();
                env.put("JAVA_HOME", jre.getAbsolutePath());
                env.put("HOME", root.getAbsolutePath());
                env.put("TMPDIR", tmp.getAbsolutePath());
                String appLib = getApplicationInfo().nativeLibraryDir;
                env.put("LD_LIBRARY_PATH", appLib + ":" + new File(jre, "lib").getAbsolutePath() + ":" + new File(jre, "lib/server").getAbsolutePath());
                env.put("PATH", new File(jre, "bin").getAbsolutePath());
                env.put("JAVA_HOME", jre.getAbsolutePath());
                env.put("GEYSER_PID_FILE", pidFile.getAbsolutePath());
                env.put("GEYSER_JAR", jar.getAbsolutePath());
                emit("启动 Geyser...", false);
                process = pb.redirectErrorStream(true).start();
                synchronized (this) {
                    processInput = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
                }
                starting = false;
                emit("Geyser 进程已启动", true);
                final Process p = process;
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line; while ((line = r.readLine()) != null) { emit(line, true); updateNotification(line); }
                }
                int exit = p.waitFor();
                synchronized (this) {
                    if (process == p) {
                        process = null;
                        processInput = null;
                    }
                }
                if (stopRequested) emit("Geyser 已停止", false);
                else emit("Geyser 已退出，代码: " + exit, false);
                finishService();
            } catch (Throwable t) {
                starting = false;
                process = null;
                processInput = null;
                if (stopRequested || t instanceof InterruptedIOException || t instanceof InterruptedException) {
                    emit("Geyser 已停止", false);
                } else {
                    emit("启动失败: " + t, false);
                }
                finishService();
            }
        });
    }

    private synchronized void sendCommand(String value) {
        String command = value == null ? "" : value.trim();
        if (command.isEmpty()) return;
        Process p = process;
        if (p == null || !p.isAlive() || processInput == null) {
            emit("无法发送指令：Geyser 进程尚未就绪", false);
            return;
        }
        try {
            processInput.write(command);
            processInput.write('\n');
            processInput.flush();
            emit("> " + command, true);
        } catch (IOException error) {
            emit("指令发送失败: " + error.getMessage(), true);
        }
    }

    private synchronized void stopGeyser() {
        Process p = process;
        if (p != null && p.isAlive()) {
            stopRequested = true;
            emit("正在向 Geyser 发送 SIGINT...", true);
            try {
                int pid = findGeyserPid();
                if (pid <= 0) throw new IOException("无法获取 Geyser 子进程 PID");
                Os.kill(pid, OsConstants.SIGINT);
            } catch (Throwable t) {
                emit("发送 SIGINT 失败: " + t, true);
            }
        } else {
            if (starting) {
                stopRequested = true;
                emit("正在取消启动...", true);
                io.shutdownNow();
                finishService();
            } else {
                emit("Geyser 未运行", false);
                finishService();
            }
        }
    }

    private int findGeyserPid() throws IOException {
        File pidFile = new File(getFilesDir(), "geyser/geyser.pid");
        if (!pidFile.isFile()) return -1;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(pidFile)))) {
            String value = r.readLine();
            return value == null ? -1 : Integer.parseInt(value.trim());
        }
    }

    private synchronized void forceStopGeyser() {
        Process p = process;
        if (p != null && p.isAlive()) {
            stopRequested = true;
            emit("正在强制结束 Geyser 进程...", true);
            p.destroyForcibly();
        } else {
            emit("Geyser 未运行", false);
            finishService();
        }
    }

    private void ensureJreExecutables(File jre) throws IOException {
        File bin = new File(jre, "bin");
        File java = new File(bin, "java");
        if (!java.isFile()) throw new IOException("JRE executable missing: " + java);
        File[] files = bin.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    file.setExecutable(true, true);
                    try { Os.chmod(file.getAbsolutePath(), 0755); } catch (Throwable ignored) { }
                }
            }
        }
        if (!java.canExecute()) throw new IOException("JRE executable permission denied: " + java);
    }

    private void unzipAsset(String name, File out) throws IOException {
        try (ZipInputStream z = new ZipInputStream(getAssets().open(name))) {
            ZipEntry e; byte[] b = new byte[65536];
            while ((e = z.getNextEntry()) != null) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("asset extraction cancelled");
                File target = new File(out, e.getName());
                String base = out.getCanonicalPath() + File.separator;
                if (!target.getCanonicalPath().startsWith(base)) throw new IOException("invalid archive path");
                if (e.isDirectory()) target.mkdirs(); else {
                    File parent = target.getParentFile(); if (parent != null) parent.mkdirs();
                    try (FileOutputStream f = new FileOutputStream(target)) { int n; while ((n = z.read(b)) >= 0) {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("asset extraction cancelled");
                        if (n > 0) f.write(b, 0, n);
                    } }
                    target.setExecutable((e.getName().startsWith("bin/") || e.getName().endsWith("/libjli.so") || e.getName().endsWith("/libjvm.so")));
                }
            }
        }
    }

    @SuppressLint("WakelockTimeout")
    private void acquireBackgroundLocks() {
        if (wakeLock != null && wakeLock.isHeld() && wifiLock != null && wifiLock.isHeld()) return;
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":GeyserCpu");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
        } catch (Throwable t) { emit("CPU 后台锁获取失败: " + t, false); }
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, getPackageName() + ":GeyserWifi");
            wifiLock.setReferenceCounted(false);
            wifiLock.acquire();
        } catch (Throwable t) { emit("Wi-Fi 后台锁获取失败: " + t, false); }
    }

    private void releaseBackgroundLocks() {
        try { if (wifiLock != null && wifiLock.isHeld()) wifiLock.release(); } catch (Throwable ignored) { }
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Throwable ignored) { }
        wifiLock = null;
        wakeLock = null;
    }

    private void finishService() {
        GeyserRuntime.markServiceStopped();
        releaseBackgroundLocks();
        stopForeground(true);
        stopSelf();
    }

    private void createChannel() {
        NotificationManager n = getSystemService(NotificationManager.class);
        n.createNotificationChannel(new NotificationChannel(CHANNEL, "Geyser", NotificationManager.IMPORTANCE_LOW));
    }
    private Notification notification(String text) {
        return new Notification.Builder(this, CHANNEL).setContentTitle("Geyser Launcher").setContentText(text).setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build();
    }
    private void updateNotification(String line) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        NotificationManager n = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        n.notify(7, notification(line.length() > 80 ? line.substring(0, 80) : line));
    }
    @Override public void onDestroy() {
        Process p = process;
        if (p != null && p.isAlive()) {
            stopRequested = true;
            try {
                int pid = findGeyserPid();
                if (pid > 0) Os.kill(pid, OsConstants.SIGINT); else p.destroy();
            } catch (Throwable ignored) { p.destroy(); }
        }
        GeyserRuntime.markServiceStopped();
        releaseBackgroundLocks();
        io.shutdownNow();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
