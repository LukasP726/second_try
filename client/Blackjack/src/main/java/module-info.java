module org.example.blackjack {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires java.logging;

    opens org.example.blackjack to javafx.fxml;
    exports org.example.blackjack;
    exports org.example.blackjack.controller;
    opens org.example.blackjack.controller to javafx.fxml;
    exports org.example.blackjack.model;
    opens org.example.blackjack.model to javafx.fxml;
    exports org.example.blackjack.client;
    opens org.example.blackjack.client to javafx.fxml;
    exports org.example.blackjack.util;
    opens org.example.blackjack.util to javafx.fxml;
}