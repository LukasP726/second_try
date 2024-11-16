package org.example.blackjack;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class CommandResponse {
    private String command;
    private String response;
    private boolean isResponseReceived = false;

    public CommandResponse(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public String getResponse() {
        return response;
    }

    public synchronized void setResponse(String response) {
        this.response = response;
        isResponseReceived = true;
        notifyAll(); // Signalizuje, že odpověď přišla
        //notify();
    }

    public synchronized String waitForResponse() throws InterruptedException {
        while (!isResponseReceived) {
            wait(); // Čeká na odpověď
        }
        return response;
    }

    public synchronized String waitForResponse2(long timeout, TimeUnit unit) throws InterruptedException {
        long endTime = System.nanoTime() + unit.toNanos(timeout);
        while (!isResponseReceived) {
            long remainingTime = endTime - System.nanoTime();
            if (remainingTime <= 0) break; // Přerušení při dosažení timeoutu
            wait(TimeUnit.NANOSECONDS.toMillis(remainingTime));
        }
        return response;
    }
}

