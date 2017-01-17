package com.afilini.opencvtest.joypadserver;

import android.graphics.Bitmap;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionListener extends Thread {
    private final int port;
    private DataOutputStream outToClient;

    public ConnectionListener(int port) {
        super("JoypadConnectionListener");
        this.port = port;
    }

    @Override
    public void run() {
        ServerSocket welcomeSocket = null;
        try {
            welcomeSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (; ; ) {
            try {
                assert welcomeSocket != null;

                Socket connectionSocket = welcomeSocket.accept();
                BufferedReader inFromClient =
                        new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendImage(Mat mat) {
        if (outToClient == null) return;

        Bitmap img = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(mat, img);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.WEBP, 70, stream);
        byte[] byteArray = stream.toByteArray();

        try {
            outToClient.writeBytes(String.format("%d\n", byteArray.length));
            outToClient.write(byteArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
