package org.example.blackjack;

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

