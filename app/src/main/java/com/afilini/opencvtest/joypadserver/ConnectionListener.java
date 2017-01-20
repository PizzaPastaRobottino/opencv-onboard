package com.afilini.opencvtest.joypadserver;

import android.graphics.Bitmap;
import android.widget.TextView;
import com.afilini.opencvtest.GR8Camera;
import com.afilini.opencvtest.ev3client.EV3ClientConnection;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionListener extends Thread {
    private final int port;
    private ObjectOutputStream outToClient;
    private BufferedReader inFromClient;
    private ServerSocket welcomeSocket;
    private GR8Camera cameraClass;
    private TextView info;

    public ConnectionListener(int port, GR8Camera cameraClass, TextView info) {
        super("JoypadConnectionListener");
        this.port = port;
        this.cameraClass = cameraClass;
        this.info = info;
    }

    @Override
    public void run() {
        welcomeSocket = null;
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
                inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                outToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public Bitmap sendImage(Mat mat) throws IOException {
        if (outToClient == null) return null;

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
        return img;
    }

    public void readCommand(EV3ClientConnection EV3Connection) {
        String command;
        while (true) {
            try {
                if (inFromClient == null) {
                    Thread.sleep(50);
                    continue;
                }
                command = inFromClient.readLine();
                cameraClass.editInfo("Received command: " + command + " from phone, sending to EV3.");
                EV3Connection.sendByte(command);
            } catch (IOException e) {
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String getIpAddress() {
        return welcomeSocket.getInetAddress().toString();
    }
}
