package com.FriedOUDON.NickUdon.fabric;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class FabricConsoleOutput {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Charset stdoutCharset = resolveCharset("stdout.encoding");
    private final Charset stderrCharset = resolveCharset("stderr.encoding");
    private final boolean directStdout = shouldUseDirectConsole(stdoutCharset);
    private final boolean directStderr = shouldUseDirectConsole(stderrCharset);

    private PrintWriter stdoutWriter;
    private PrintWriter stderrWriter;

    void sendInfo(MinecraftServer server, Text text) {
        if (!directStdout) {
            server.sendMessage(text);
            return;
        }
        stdout().println(prefix("INFO") + text.getString());
    }

    void sendInfo(Text text) {
        stdout().println(prefix("INFO") + text.getString());
    }

    void sendError(Text text) {
        stderr().println(prefix("ERROR") + text.getString());
    }

    boolean shouldBypassServerLogger() {
        return directStdout;
    }

    boolean shouldBypassErrorLogger() {
        return directStderr;
    }

    private PrintWriter stdout() {
        if (stdoutWriter == null) {
            stdoutWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FileDescriptor.out), stdoutCharset), true);
        }
        return stdoutWriter;
    }

    private PrintWriter stderr() {
        if (stderrWriter == null) {
            stderrWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(FileDescriptor.err), stderrCharset), true);
        }
        return stderrWriter;
    }

    private static String prefix(String level) {
        return "[" + LocalTime.now().format(TIME_FORMAT) + "] [NickUdon/" + level + "]: ";
    }

    private static Charset resolveCharset(String propertyName) {
        String configured = System.getProperty(propertyName);
        if (configured != null && !configured.isBlank()) {
            return Charset.forName(configured);
        }

        String nativeEncoding = System.getProperty("native.encoding");
        if (nativeEncoding != null && !nativeEncoding.isBlank()) {
            return Charset.forName(nativeEncoding);
        }

        return Charset.defaultCharset();
    }

    private static boolean shouldUseDirectConsole(Charset charset) {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT).contains("windows") && !StandardCharsets.UTF_8.equals(charset);
    }
}
