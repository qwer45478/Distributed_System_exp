package exp.exp2.streamchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MyStreamSocket extends Socket {
    private final Socket delegate;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Object readLock = new Object();
    private final Object writeLock = new Object();

    public MyStreamSocket(String host, int port) throws IOException {
        super(host, port);
        this.delegate = null;
        this.in = new DataInputStream(super.getInputStream());
        this.out = new DataOutputStream(super.getOutputStream());
    }

    public MyStreamSocket(InetAddress host, int port) throws IOException {
        super(host, port);
        this.delegate = null;
        this.in = new DataInputStream(super.getInputStream());
        this.out = new DataOutputStream(super.getOutputStream());
    }

    public MyStreamSocket(Socket acceptedSocket) throws IOException {
        super();
        this.delegate = acceptedSocket;
        this.in = new DataInputStream(acceptedSocket.getInputStream());
        this.out = new DataOutputStream(acceptedSocket.getOutputStream());
    }

    public void sendMessage(String message) throws IOException {
        synchronized (writeLock) {
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
        }
    }

    public String receiveMessage() throws IOException {
        synchronized (readLock) {
            int len = in.readInt();
            byte[] data = new byte[len];
            in.readFully(data);
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        IOException closeException = null;
        try {
            in.close();
        } catch (IOException e) {
            closeException = e;
        }
        try {
            out.close();
        } catch (IOException e) {
            if (closeException == null) {
                closeException = e;
            }
        }

        if (delegate != null) {
            delegate.close();
        } else {
            super.close();
        }

        if (closeException != null) {
            throw closeException;
        }
    }
}
