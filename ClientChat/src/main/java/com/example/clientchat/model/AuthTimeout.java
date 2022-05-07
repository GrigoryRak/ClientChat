package com.example.clientchat.model;

import com.example.clientchat.ClientChat;
import com.example.clientchat.controllers.AuthController;
import com.example.clientchat.dialogs.Dialogs;
import javafx.application.Platform;

import java.util.Date;
import java.util.TimerTask;

public class AuthTimeout extends TimerTask {

    @Override
    public void run() {
        System.out.println("Timer timeoutTask started at: " + new Date());

//        AuthController authController = new AuthController();
//        authController.initializeMessageHandler();

//        if (!authController.isConnectedToServer()) {
        timeoutTask();
        Platform.runLater(() -> {
            Dialogs.AuthTimeout.AUTH_TIMEOUT.show();
            ClientChat.getInstance().getAuthStage().close();
            ClientChat.getInstance().getChatStage().close();
        });
        System.out.println("Timer timeoutTask finished at: " + new Date());
//        }
    }

    private void timeoutTask() {
        try {
            // На выполнение задачи требуется 10 секунд
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
