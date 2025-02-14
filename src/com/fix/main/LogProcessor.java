package com.fix.main;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class LogProcessor {
    private static final String FIX_MESSAGE_START = "35=";
    private static final String FIX_MESSAGE_KEY = "[35=";
    private final FixDictionary dict;
    private final Set<Character> whiteChars;


    private boolean inMsgFromPrevLine = false;
    private String msg = "";

    public LogProcessor(FixDictionary dict) {
        this.dict = dict;
        this.whiteChars = new HashSet<>();
        whiteChars.add ('\n');
        whiteChars.add ('\r');
        whiteChars.add ('\t');
        whiteChars.add (' ');
    }
    public void processLine(String line ) throws IOException {
        // String result = ""; // used during debugging to see  what is displayed
//        if (line  == null) {
//            return;
//        }
        char[] chars = new char[line.length()];
        line.getChars(0, line.length(),  chars, 0 );
        int n = 0;
        for(int i = 0; i < chars.length; i++ ) {
            char cc = chars[i];

            if (!inMsgFromPrevLine && cc != '[') {
                n++;
                continue;
            }
            if (n > 0) {
                //result +=line.substring(i-n, i++)
                System.out.print(line.substring((i-n), i++)); // do not include '['
                n = 0;
            } else ++i;

            // go on until ']'
            int j = i; //inMsgFromPrevLine ? i : i + 1;
            for(; j<line.length(); j++){
                cc = chars[j];

                if (whiteChars.contains(cc) )
                    continue;
                if (cc == ']')
                    break;
                msg += "" + cc;
            }
            i = j;

            inMsgFromPrevLine = ( cc != ']');
            if (!inMsgFromPrevLine) {
                if (msg.startsWith(FIX_MESSAGE_START)) {
                    // result += this.dict.explainMessage(msg);
                    System.out.println("\n" + this.dict.explainMessage(msg));
                } else {
                    // result += "[" + msg + "]" ;
                    System.out.print("[" + msg + "]");
                }
                msg = "";
            }
        }
    }

    public  static void main (String [] args ) throws IOException {
        String line = "2025-02-13 01:38:12,752 INFO [i-0TradesExecutor-Thread-1] rfp-fix - OUT --> [35=AE|34=154|1=1594742|568=opos-6177f|855=1|39=11|55=USDJPY|17=90276239400|527=902762394|7402=4.0E-4|552=1|37=902762394|11=o-1739348386202-7|44=153.611|40=2|60=1739348386205|54=1|38=1000.0|800=1000.0|14=1000.0|4=1|6=153.613|483=1739348597428|31=153.613|32=1000.0|132=153.598|133=153.611] [ttdemoNY, 10, rdy]";
        FixDictionary dict = new FixDictionary();
        dict.init();

        LogProcessor pr = new LogProcessor(dict);
        pr.processLine(line);
    }
}
