package com.fix.main;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class LogTailer {
    private static final int QUEUE_CAPACITY_RESPONSIVE = 3;
    private static final int QUEUE_CAPACITY_OPTIMISED = 50;
    private static final int QUEUE_CAPACITY_HEAVY_LOAD0 = 500;
    private static String logFile;
    private static  LogProcessor processor = null ;
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY_RESPONSIVE); ;
    private static volatile boolean running = true;

    private static boolean currentContentOnly;


    public static void main(String[] args) throws IOException {
        LogTailer logTailer = new LogTailer();
        logTailer.run( args );
    }


    /** Shutdown gracefully */
    /*synchronized */public static void stop() {
        if (!running)
            return;
         System.out.println("Shutdown gracefully signaled");
        running = false;
    }


    /** Creates producer thread for stdin */
    private   Thread createStdInProducer() {
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
                running = false;

                System.out.println("Pipeline processing thread exited gracefully");

            } catch (IOException  e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Creates producer thread for log file */
    private  Thread createLogFileProducer() {
        return new Thread(() -> {
            try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
                long filePointer = currentContentOnly ? 0 : raf.length(); // Start at the end (like `tail -f`)
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
                        if (currentContentOnly)
                            break; // wait a bit first to give change other thread  output to finish first
                    }
                }
                System.out.println("Log file processing thread exited gracefully");
                stop();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    private   Thread createConsumer() {
        return new Thread(() -> {
            try {
                while (running) {
                    String line = queue.poll (777, TimeUnit.MILLISECONDS);
                    if (line == null)
                        break;
                    try {
                        processor.processLine(line);
                    } catch (IOException e) {
                       e.printStackTrace();
                    }
                }
                System.out.println("Consumer thread exited gracefully");
                stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private  void initDictionary() throws InvalidObjectException {
        FixDictionary dict = new FixDictionary();

        dict.init();
        processor = new LogProcessor(dict);
    }

    private  void parseCommandLine(String []args) throws IOException, IllegalArgumentException {
        if(args.length > 1) {
            currentContentOnly = ("-curr".equalsIgnoreCase(args[0]));
            logFile = args[1];
        } else if (args.length > 0) {
            logFile = args[0];
        } else {
            throw new IllegalArgumentException("Command line param expected");
        }

        // Resolve symlink if it's a symlink
        Path path = new File(logFile).toPath();
        if (Files.isSymbolicLink(path)) {
            logFile = Files.readSymbolicLink(path).toAbsolutePath().toString();
        }
    }

    public  void run (String [] args) throws IOException {
        boolean isPipedInput  = System.console() == null; //System.in.available() > 0;
        if (!isPipedInput) {
            parseCommandLine(args);
        }

        // initialize internal objects
        initDictionary();

        // Start producer and consumer threads
        Thread producer = isPipedInput ? createStdInProducer() : createLogFileProducer();
        Thread consumer = createConsumer();
        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(LogTailer::stop));

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
}
