package rdpl.send;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.zip.Deflater;

/**
 *
 * @author Robotics
 */
public class RdplSend {

    static byte[] htonl(int i) {
        return ByteBuffer.allocate(4).putInt(i).order(ByteOrder.BIG_ENDIAN).array();
    }

    static byte[] loadFile(String path) {
        try {
            byte[] data = Files.readAllBytes(new File(path).toPath());

            Deflater deflater = new Deflater(5);
            deflater.setInput(data);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1 << 20);

            deflater.finish();
            byte[] buffer = new byte[1 << 16];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            return outputStream.toByteArray();

        } catch (IOException ex) {
            return null;
        }
    }

    static boolean trySend(byte[] content) {
        try (Socket sock = new Socket("roborio-1511.local", 1511)) {
            OutputStream o = sock.getOutputStream();
            o.write(htonl(content.length));
            o.write(content);
        } catch (IOException e) {
            System.err.println("Failed to write");
            return false;
        }
        System.err.println("Write successful");
        return true;
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) {
            System.err.println("No file to act on");
            return;
        }

        String absPath = new File(args[0]).getAbsolutePath();
        byte[] content = loadFile(args[0]);
        if (content == null) {
            System.err.format("Could not read file %s\n", absPath);
            return;
        }

        System.err.format("Loaded file at %s. Compressed size is %d.\n", absPath, content.length);

        long millis = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            if (trySend(content)) {
                break;
            }
            System.err.println("Send failed, retrying soon");
            Thread.sleep(3000);
        }
        System.err.format("Complete (time %d)\n", System.currentTimeMillis() - millis);
    }
}
