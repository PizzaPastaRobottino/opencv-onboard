package com.afilini.opencvtest.ev3client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class EV3ClientConnection extends Thread {

    private InetAddress IP;
    private int port;
    private Socket connection;
    private DataOutputStream sendData;

    public EV3ClientConnection(InetAddress IP, int port) {
        this.IP = IP;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            createConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createConnection() throws IOException {
        connection = new Socket(IP, port);
        sendData = new DataOutputStream(connection.getOutputStream());
    }

    public void sendByte(String type) throws IOException {

        switch (type) {
            case "AVANTI":
                sendData.writeByte(0b00000001);
                break;

            case "INDIETRO":
                sendData.writeByte(0b01000001);
                break;

            case "GIRA_DESTRA":
                sendData.writeByte(0b10111110);
                break;

            case "GIRA_SINITRA":
                sendData.writeByte(0b10011110);
                break;

            case "IMPOSTA":
                sendData.writeByte(0b11000010);
                break;
        }
    }
}
