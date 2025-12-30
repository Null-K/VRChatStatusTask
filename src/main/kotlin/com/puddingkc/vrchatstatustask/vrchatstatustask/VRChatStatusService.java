package com.puddingkc.vrchatstatustask.vrchatstatustask;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.transport.OSCPortOut;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.puddingkc.vrchatstatustask.vrchatstatustask.configs.VRChatStatusSettings;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service(Service.Level.PROJECT)
public final class VRChatStatusService implements Disposable {

    private static final Logger LOG = Logger.getInstance(VRChatStatusService.class);

    private final Project project;
    private final long startTime = System.currentTimeMillis();
    private volatile OSCPortOut sender;
    private ScheduledFuture<?> task;

    public VRChatStatusService(Project project) {
        this.project = project;
    }

    public void start() {
        if (task != null && !task.isCancelled()) return;

        VRChatStatusSettings settings = VRChatStatusSettings.getInstance();
        int interval = settings.updateInterval;

        ensureSender();
        task = AppExecutorUtil.getAppScheduledExecutorService()
                .scheduleWithFixedDelay(this::updateVRChat, interval, interval, TimeUnit.SECONDS);
    }

    public void restart() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
        closeSender();

        VRChatStatusSettings settings = VRChatStatusSettings.getInstance();
        if (settings.enabledOsc) {
            start();
        } else {
            sendOsc("");
        }
    }

    private synchronized void ensureSender() {
        if (sender != null) return;

        VRChatStatusSettings settings = VRChatStatusSettings.getInstance();
        try {
            sender = new OSCPortOut(InetAddress.getByName(settings.oscAddress), settings.oscPort);
        } catch (IOException e) {
            LOG.warn("OSC 初始化失败: " + e.getMessage());
        }
    }

    private void updateVRChat() {
        if (project.isDisposed()) return;

        ReadAction.run(() -> {
            if (project.isDisposed()) return;

            FileEditorManager fem = FileEditorManager.getInstance(project);
            VirtualFile[] selectedFiles = fem.getSelectedFiles();
            String currentFileName = (selectedFiles.length > 0) ? selectedFiles[0].getName() : "Idle";

            int errors = countHighlights(HighlightSeverity.ERROR);
            int warnings = countHighlights(HighlightSeverity.WARNING);

            String currentLine = getCurrentLineContent();
            int lineNumber = getCurrentLineNumber();

            String msg = getString(currentFileName, errors, warnings, currentLine, lineNumber);
            sendOsc(msg);
        });
    }

    private String getCurrentLineContent() {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return "";

        Document document = editor.getDocument();
        Caret caret = editor.getCaretModel().getPrimaryCaret();
        int lineNumber = caret.getLogicalPosition().line;

        if (lineNumber >= document.getLineCount()) return "";

        int lineStart = document.getLineStartOffset(lineNumber);
        int lineEnd = document.getLineEndOffset(lineNumber);
        return document.getText().substring(lineStart, lineEnd).trim();
    }

    private int getCurrentLineNumber() {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return 0;

        Caret caret = editor.getCaretModel().getPrimaryCaret();
        return caret.getLogicalPosition().line + 1;
    }

    private @NotNull String getString(String currentFileName, int errors, int warnings, String currentLine, int lineNumber) {
        VRChatStatusSettings settings = VRChatStatusSettings.getInstance();
        String projectName = project.getName();

        if (settings.textCropping) {
            if (currentFileName.length() > 19) {
                currentFileName = currentFileName.substring(0, 19) + "...";
            }
            if (projectName.length() > 19) {
                projectName = projectName.substring(0, 19) + "...";
            }
            if (currentLine.length() > 35) {
                currentLine = currentLine.substring(0, 35) + "...";
            }
        }

        String template = settings.messageTemplate;
        return template
                .replace("{project}", projectName)
                .replace("{file}", currentFileName)
                .replace("{errors}", String.valueOf(errors))
                .replace("{warnings}", String.valueOf(warnings))
                .replace("{uptime}", getDurationString())
                .replace("{line}", currentLine)
                .replace("{lineNum}", String.valueOf(lineNumber));
    }

    private int countHighlights(HighlightSeverity severity) {
        FileEditorManager fem = FileEditorManager.getInstance(project);
        DaemonCodeAnalyzerEx.getInstanceEx(project);

        final int[] total = {0};

        for (VirtualFile file : fem.getOpenFiles()) {
            Document doc = FileDocumentManager.getInstance().getDocument(file);
            if (doc == null) continue;

            DaemonCodeAnalyzerEx.processHighlights(doc, project, severity, 0, doc.getTextLength(), info -> {
                        if (info != null && info.getSeverity() == severity) {
                            total[0]++;
                        }
                        return true;
                    }
            );
        }

        return total[0];
    }

    private void sendOsc(String text) {
        if (sender == null) ensureSender();
        if (sender == null) return;
        try {
            sender.send(new OSCMessage("/chatbox/input", Arrays.asList(text, true, false)));
        } catch (Exception e) {
            closeSender();
        }
    }

    private synchronized void closeSender() {
        if (sender != null) {
            try { sender.close(); } catch (IOException ignored) {}
            sender = null;
        }
    }

    private String getDurationString() {
        long sec = (System.currentTimeMillis() - startTime) / 1000;
        return String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
    }

    @Override
    public void dispose() {
        if (task != null) {
            task.cancel(true);
        }
        closeSender();
    }
}
