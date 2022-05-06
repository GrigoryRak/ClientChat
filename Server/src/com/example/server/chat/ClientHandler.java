package com.example.server.chat;

import com.example.command.Command;
import com.example.command.CommandType;
import com.example.command.commands.commands.AuthCommandData;
import com.example.command.commands.commands.PrivateMessageCommandData;
import com.example.command.commands.commands.PublicMessageCommandData;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler {

    private MyServer server;
    private final Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String userName;

    public ClientHandler(MyServer server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    public void handle() throws IOException {
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        new Thread(() -> {
            try {
                authenticate();
                readMessages();
            } catch (IOException e) {
                System.err.println("Failed to process message from client");
                e.printStackTrace();
            } finally {
                try {
                    closeConnection();
                } catch (IOException e) {
                    System.err.println("Failed to close connection");
                }
            }
        }).start();
    }

//    https://ru.stackoverflow.com/questions/536919/java-%d0%b2%d1%8b%d0%bf%d0%be%d0%bb%d0%bd%d0%b5%d0%bd%d0%b8%d0%b5-%d0%b4%d0%b5%d0%b9%d1%81%d1%82%d0%b2%d0%b8%d0%b9-%d1%80%d0%b0%d0%b7-%d0%b2-%d0%bc%d0%b8%d0%bd%d1%83%d1%82%d1%83-%d0%b8-%d0%b1%d0%b5%d1%81%d0%ba%d0%be%d0%bd%d0%b5%d1%87%d0%bd%d0%be-%d0%b4%d0%be%d0%bb%d0%b3%d0%be/537035#537035
    private void authenticate() throws IOException {
        while (true) {
            Command command = readCommand();

            if (command == null) {
                continue;
            }

            if (command.getType() == CommandType.AUTH) {
                AuthCommandData data = (AuthCommandData) command.getData();
                String login = data.getLogin();
                String password = data.getPassword();
                String userName = this.server.getAuthService().getUsernameByLoginAndPassword(login, password);
                if (userName == null) {
                    sendCommand(Command.errorCommand("Некорректные имя пользователя или пароль"));
                } else if (server.isUserNameBusy(userName)) {
                    sendCommand(Command.errorCommand("Такой пользователь уже существует"));
                } else {
                    this.userName = userName;
                    sendCommand(Command.authOkCommand(userName));
                    server.subscribe(this);
                    return;
                }
            }
        }
    }



    private Command readCommand() throws IOException {
        Command command = null;

        try {
            command = (Command) inputStream.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to read Command class");
            e.printStackTrace();
        }

        return command;
    }

    private void readMessages() throws IOException {
        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }
            switch (command.getType()) {
                case PRIVATE_MESSAGE: {
                    PrivateMessageCommandData data = (PrivateMessageCommandData) command.getData();
                    String receiver = data.getReceiver();
                    String privateMessage = data.getMessage();
                    server.sendPrivateMessage(this, receiver, privateMessage);
                    break;
                }
                case PUBLIC_MESSAGE:
                    PublicMessageCommandData data = (PublicMessageCommandData) command.getData();
                    processMessage(data.getMessage());
                    break;
            }
        }
    }

    private void processMessage(String message) throws IOException {
        this.server.broadcastMessage(message, this);
    }

    public void sendCommand(Command command) throws IOException {
        outputStream.writeObject(command);
    }

    private void closeConnection() throws IOException {
        outputStream.close();
        inputStream.close();
        server.unsubscribe(this);
        clientSocket.close();
    }

    public String getUserName() {
        return userName;
    }
}
