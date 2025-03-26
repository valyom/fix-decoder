package com.fix.main;

import com.fix.Config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

public class FixDecoder {

    private static final int QUEUE_CAPACITY_RESPONSIVE = 3;
    private static final int QUEUE_CAPACITY_OPTIMISED = 50;
    private static final int QUEUE_CAPACITY_HEAVY_LOAD0 = 500;
    private static String logFile;
    private static  LogProcessor processor = null ;
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY_RESPONSIVE); ;
    private static volatile boolean running = true;

    private static boolean currentContentOnly;
    private static boolean keepQuiet = true;
    private static ExecutorService executor = null;
    private final static CountDownLatch latch = new CountDownLatch(1);

    public static void main (String[] args) throws InterruptedException, IOException {
//        args = " -f 39 -curr -key 41067400 /home/victor/fx.msg".split(" ");
//        args = "-m [35=D|34=271133|1=41067400|55=US30|54=1|58=TT-WebTrader|60=1742590775499|59=4|38=20.0|40=1|583=p-1742581465335-1__25_X|11=cp-1742590775499-1|18=1|44=42018.75]  -f 39 -curr -key 41067400 /home/victor/fx.msg".split(" ");
        boolean isPipedInput =  System.console() == null; //System.in.available() > 0;
        // if (!isPipedInput)
            parseParam(args);

        FixDictionary dict = new FixDictionary().init();
        if (Config.FIELD_TO_EXPLAIN != null) {
            System.out.println( dict.explainField(Config.FIELD_TO_EXPLAIN));
            return;
        }

        processor = new LogProcessor(dict);

        executor = Executors.newFixedThreadPool(2);
        executor.execute(FixDecoder::consume);

        if (Config.SINGLE_MESSAGE !=null )
            executor.execute(FixDecoder::produceFromSingleMessage);
        else
            executor.execute(isPipedInput ? FixDecoder::produceFromStdIn : FixDecoder::produceFromFile);

        Runtime.getRuntime().addShutdownHook(new Thread(FixDecoder::shutDownHook));// Add shutdown hook for graceful shutdown

        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        log("Exiting main!");
    }

    /**  producer  for stdin */
    private static void produceFromStdIn() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while (running) {
                line = reader.readLine();
                if (line != null) {
                    while (running && !queue.offer(line)) {
                        Thread.sleep(100);
                    };
                }
            }
            log("FIXDecoder : Pipeline processing thread exited gracefully");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private static void produceFromSingleMessage()  {
        String message = Config.SINGLE_MESSAGE;
        try {
            queue.offer(message);
            Thread.sleep(50);
            queue.offer("Finished");// signal consumer to exit
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }
    /**  producer  log file */
    private static void produceFromFile() {
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
                    while (running  && (line = raf.readLine()) != null) {
                        if (Config.KEYWORD != null && !line.contains(Config.KEYWORD)) {
                            continue;
                        }

                        while( running && !queue.offer(line) ) {
                            Thread.sleep(50);
                        }
                    }
                    filePointer = raf.getFilePointer();
                    if(currentContentOnly && filePointer == fileLength) {
                        // whait consumer to finish
                        while (queue.size() > 0) {
                            Thread.sleep(50); // flush consumer job
                        }
                        queue.offer("Finished");// signal consumer to exit
                        break;
                    }
                } else {
                    Thread.sleep(100); // Avoid busy-waiting
                }
            }

            // latch.countDown();
            log("FIXDecoder : Producer exited gracefully");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private  static void consume() {
        try {
            while (running) {
                String line = queue.poll (777, TimeUnit.MILLISECONDS);
                if (line == null) {
//                    if (latch.getCount() == 0) {
//                        break;
//                    }
                    continue;
                }
                if ("Finished".equals(line))
                    break;
                try {
                    processor.processLine(line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            log("FIXDecoder : Consumer exited gracefully");
            shutDownHook();
        } catch (InterruptedException e) {
            log("FIXDecoder : Consumer interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private static void shutDownHook ()  {
        if (executor == null)
            return;
        if (!running)
            return;
        log("FIXDecoder : Shutdown signal received.");

        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                log("FIXDecoder : Forcing shutdown...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        log("Shutdown complete!");
    }

    private static void parseParam(String []args) throws IOException, IllegalArgumentException {
        boolean takeKeyWord = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.trim().isEmpty())
                continue;
            if (arg.equals("-m")) {
                int j = i+1;
                if (j < args.length) {
                    Config.KEYWORD = null;
                    Config.FIELD_TO_EXPLAIN = null;
                    logFile = null;
                    Config.SINGLE_MESSAGE = args[j];
                    return;
                }
            } else if (arg.equals("-f")) {
                int j = i+1;
                if (j < args.length) {
                    Config.KEYWORD = null;
                    logFile = null;
                    Config.FIELD_TO_EXPLAIN = args[j];
                    return;
                }
            } else if (arg.equals("-key") & Config.KEYWORD== null) {
                takeKeyWord = true;
            } else if (takeKeyWord && !arg.startsWith("-")) {
                Config.KEYWORD=arg;
                takeKeyWord = false;
            } else if(!arg.startsWith("-")) {
                logFile = arg;
            } else if ("-curr".equalsIgnoreCase(arg)) {
                currentContentOnly = true;
            } else if ("-verbose".equalsIgnoreCase(arg)) {
                keepQuiet = false;
            }
        }


//
//        for(String arg :args) {
//            if (arg.trim().isEmpty())
//                continue;
//            if (arg.equals("-key") & Config.KEYWORD== null) {
//                takeKeyWord = true;
//            } else if (takeKeyWord && !arg.startsWith("-")) {
//                Config.KEYWORD=arg;
//                takeKeyWord = false;
//            } else if(!arg.startsWith("-")) {
//                logFile = arg;
//            } else if ("-curr".equalsIgnoreCase(arg)) {
//                currentContentOnly = true;
//            } else if ("-verbose".equalsIgnoreCase(arg)) {
//                keepQuiet = false;
//            }
//        }

        // Resolve symlink if it's a symlink
        Path path = new File(logFile).toPath();
        if (Files.isSymbolicLink(path)) {
            logFile = Files.readSymbolicLink(path).toAbsolutePath().toString();
        }
    }

    private static void log(String str) {
        if (!keepQuiet)
            System.out.println(str);
    }
}
