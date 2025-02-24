package com.fix.main;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
public class LogTailer {
    private static final int QUEUE_CAPACITY_RESPONSIVE = 3;
    private static final int QUEUE_CAPACITY_OPTIMISED = 50;
    private static final int QUEUE_CAPACITY_HEAVY_LOAD0 = 500;
    private static String logFile;
    private static  LogProcessor processor = null ;
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY_RESPONSIVE); ;
    private static volatile boolean running = true;

    public static void main(String[] args) throws IOException {
        FixDictionary dict = new FixDictionary();

        dict.init();
        if(!dict.isInitialized()) {
            System.out.println("Can't initialize FIX dictionary! Exit.");

            return;
        }
        processor = new LogProcessor(dict);

        boolean isPipedInput  = System.console() == null; //System.in.available() > 0;
        if (!isPipedInput && args.length > 0) {
            logFile = args[0];

            // Resolve symlink if it's a symlink
            Path path = new File(logFile).toPath();
            if (Files.isSymbolicLink(path)) {
                logFile = Files.readSymbolicLink(path).toAbsolutePath().toString();
                /// System.out.println("Resolved symlink: " + logFile);
            }
            // System.out.println("Using log file: " + logFile);
        }

        // Graceful shutdown hook
        Thread shutdownListener = new Thread(LogTailer::stop);
        shutdownListener.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(shutdownListener);

        // Start producer thread
        Thread producer = isPipedInput ? createStdInProducer() : createLogFileProducer();

        // Start consumer thread
        Thread consumer = createConsumer();

        producer.setDaemon(true);
        consumer.setDaemon(true);

        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /** Shutdown gracefully */
    public static void stop() {
        //  System.out.println("Shutdown gracefully");
        running = false;
    }


    /** Creates producer thread for stdin */
    private static  Thread createStdInProducer() {
        return new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while (running) {
                    line = reader.readLine();
                    if (line != null) {
                        while (!queue.offer(line)) {
                            Thread.sleep(100);
                        };
                    }
                }
            } catch (IOException  e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Creates producer thread for log file */
    private static Thread createLogFileProducer() {
        return new Thread(() -> {
            try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
                long filePointer = raf.length(); // Start at the end (like `tail -f`)

                while (running) {
                    long fileLength = new File(logFile).length();

                    if (fileLength < filePointer) {
                        // File was truncated or rotated, reset pointer
                        filePointer = 0;
                        raf.seek(filePointer);
                    }

                    if (filePointer < fileLength) {
                        raf.seek(filePointer);
                        String line;
                        while ((line = raf.readLine()) != null) {
                           while(  !queue.offer(line) ) {
                               Thread.sleep(100);
                           }
                        }
                        filePointer = raf.getFilePointer();
                    } else {
                        Thread.sleep(200); // Avoid busy-waiting
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    private  static Thread createConsumer() {
        return new Thread(() -> {
            try {
                while (running) {
                    String line = queue.take();
                    try {
                        processor.processLine(line);
                    } catch (IOException e) {
                       e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
