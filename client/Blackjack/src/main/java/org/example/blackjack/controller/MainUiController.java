package org.example.blackjack.controller;

import javafx.application.Platform;

public class MainUiController {
    public void updateUi(Runnable uiTask) {
        if (Platform.isFxApplicationThread()) {
            uiTask.run();
        } else {
            Platform.runLater(uiTask);
        }
    }
}

