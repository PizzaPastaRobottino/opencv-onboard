package com.afilini.opencvtest.joypadserver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionListener extends Thread {
    private final int port;
    private ObjectOutputStream outToClient;

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

        while (welcomeSocket == null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (; ; ) {
            try {
                Socket connectionSocket = welcomeSocket.accept();
                BufferedReader inFromClient =
                        new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                outToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendImage(Mat mat) throws IOException {
        if (outToClient == null) return;

        Bitmap img = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(mat, img);

        int currentHeight = mat.rows();
        int currentWidth = mat.cols();

        outToClient.writeInt(currentWidth);
        outToClient.writeInt(currentHeight);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.WEBP, 30, stream);
        BitmapDataObject bitmapDataObject = new BitmapDataObject();
        bitmapDataObject.imageByteArray = stream.toByteArray();

        outToClient.writeObject(bitmapDataObject);
    }

    private Bitmap readImage(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int sourceWidth = in.readInt();
        int sourceHeight = in.readInt();

        BitmapDataObject bitmapDataObject = (BitmapDataObject) in.readObject();
        return BitmapFactory.decodeByteArray(bitmapDataObject.imageByteArray, 0, bitmapDataObject.imageByteArray.length);
    }
}
