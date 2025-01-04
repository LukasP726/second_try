module org.example.blackjack {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires java.logging;

    opens org.example.blackjack to javafx.fxml;
    exports org.example.blackjack;
}