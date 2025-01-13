package org.example.blackjack.util;

import java.io.IOException;
import java.util.logging.*;

public class ClientLogger {
    private static final Logger logger = Logger.getLogger(ClientLogger.class.getName());

    static {
        try {
            // Vytvoření file handleru pro zápis logů do souboru
            FileHandler fileHandler = new FileHandler("client.log", true); // true pro přidávání na konec souboru
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL); // Nastaví úroveň logování
        } catch (IOException e) {
            System.err.println("Chyba při nastavování logování: " + e.getMessage());
        }
    }

    public static void logInfo(String message) {
        logger.info(message);
    }

    public static void logWarning(String message) {
        logger.warning(message);
    }

    public static void logSevere(String message) {
        logger.severe(message);
    }
}
