package org.ps2.ui;

import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.filesys.debug.DebugInterfaceBase;

import java.util.Arrays;


public class ClientLogger extends DebugInterfaceBase {
    private static final String[] SUCCESS_STRINGS = {
            "logged on"
    };

    private static final String[] FAIL_STRINGS = {
            "closed session"
    };

    private TextFlow loggingArea;

    public void setLoggingArea(TextFlow loggingArea) {
        this.loggingArea = loggingArea;
    }

    @Override
    public void debugPrint(String str, int level) {
        if (level <= getLogLevel()) {
            System.out.print(str);
            printToLoggingArea(str);
        }
    }

    @Override
    public void debugPrintln(String str, int level) {
        if (level <= getLogLevel()) {
            System.out.println(str);
            printToLoggingArea(str);
        }
    }

    private void printToLoggingArea(String str) {
        if (loggingArea != null) {
            Platform.runLater(() -> {
                Text newText = new Text();
                newText.setText(str + '\n');
                newText.setWrappingWidth(300);
                loggingArea.getChildren().add(newText);

                if (Arrays.stream(SUCCESS_STRINGS).anyMatch(str.toLowerCase()::contains)) {
                    newText.setStyle("-fx-fill: -fx-color-success");
                } else if (Arrays.stream(FAIL_STRINGS).anyMatch(str.toLowerCase()::contains)) {
                    newText.setStyle("-fx-fill: -fx-color-fail");
                }
            });
        }
    }
}
