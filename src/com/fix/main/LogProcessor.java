package com.fix.main;

import com.fix.Config;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.HashSet;
import java.util.Set;

public class LogProcessor {
    private static final String FIX_MESSAGE_START = "35=";
    private static final String FIX_MESSAGE_KEY = "[35=";
    private final FixDictionary dict;

    private boolean inMsgFromPrevLine = false;
    private String msg = "";

    public String getMsg() {
        return msg;
    }

    public LogProcessor(FixDictionary dict) {
        this.dict = dict;
    }
    public void processLine(String line ) throws IOException {

        String rest = line.trim();
        if(rest.startsWith("#")) {
            //System.out.print(line);
            return;
        }
        final String keyWordInColor = Config.ANSI_RED + Config.KEYWORD + Config.ANSI_RESET;
        while (!rest.isEmpty()) {
            if (inMsgFromPrevLine) {
                int idxE = rest.indexOf(']');
                if (idxE <= 0 ) {
                    msg += rest;
                    return;
                }
                msg += rest.substring(0, idxE);

                rest = rest.substring(idxE+1);
                if (msg.startsWith(FIX_MESSAGE_START)) {
                    output("\n" + this.dict.explainMessage(msg));
                } else {
                    System.out.print("[" + msg + "]");
                }

                msg = "";
                inMsgFromPrevLine= false;
            }
            int idxB = rest.indexOf('[');
            if (idxB >= 0) {
                String s1 = rest.substring(0, idxB);
                if (Config.KEYWORD != null && s1.contains(Config.KEYWORD)) {
                    s1 = s1.replace(Config.KEYWORD,  keyWordInColor );
                } else {
                    System.out.print(s1); // do not include '['
                }
                rest = rest.substring(idxB+1);
                int idxE = rest.indexOf(']');
                if (idxE < 0) {
                    System.out.print("[" + rest);
                   // msg +=rest;
                   // inMsgFromPrevLine = true;
                    break;
                }
                msg = rest.substring(0, idxE);
                rest = rest.substring(idxE+1);
                //if (msg.startsWith(FIX_MESSAGE_START)) {
                    String explained = this.dict.explainMessage(msg);
                    output(explained);
//                } else {
//                    System.out.print("[" + msg + "]");
//                }
                msg = "";
            } else {

                if (Config.KEYWORD != null && rest.contains(Config.KEYWORD)) {
                    rest = rest.replace(Config.KEYWORD,  keyWordInColor );
                }
                System.out.print(rest);
                rest = "";
            }
        }
        if (!inMsgFromPrevLine)  output("");
    }

    private long k = 0;
    private void output (String line) {
        System.out.println(line);
        //if (++k % 121 == 0) {
            System.out.flush();
        //}
    }

    public  static void main (String [] args ) throws IOException {
        String  splipagelog = "2025-03-03 10:00:27,727 [INFO ] OUT --> [35=8|34=287363|1=762933520|37=106936956|40=1|39=2|55=USDJPY|31=150.41500000000002|38=2000000.0|151=1900000.0|14=100000.0|54=2|59=4|60=1741014027727] [ttDemouk, 1, rdy] [i-6fomo-trading-Thread-1]\n" +
                "2025-03-03 10:00:27,875 [INFO ] OUT --> [35=AE|34=287364|1=762933520|568=cpos-262069ab2|855=2|39=2|55=USDJPY|17=10693695605|527=106936581|7501=62, Position closed partially|7401=-8892.194098429978|552=2|37=106936581|11=p-1741008633923-87__1025_X|44=151.079|40=1|60=1741008633924|54=1|38=2000000.0|800=2000000.0|14=2000000.0|4=1|6=151.0846|483=1741008634020|31=151.0846|32=2000000.0|132=151.067|133=151.079|552=2|37=106936956|11=cp-1741014027502-137|44=150.586|40=1|60=1741014027783|54=2|38=2000000.0|800=2000000.0|14=100000.0|4=2|6=150.41500000000002|483=1741014027783|151=1900000.0|31=150.415|32=100000.0|132=150.586|133=150.604] [ttDemouk, 1, rdy] [i-6fomo-trading-Thread-1]\n" +
                "2025-03-03 10:00:30,339 [INFO ] OUT --> [35=AE|34=287376|1=762933520|568=cpos-262069ab2|855=1|39=2|55=USDJPY|17=10693695600|527=106936581|7401=-444.66|552=1|37=106936956|11=cp-1741014027502-137|44=150.586|40=1|60=1741014027503|54=2|38=2000000.0|800=2000000.0|14=1900000.0|4=1|6=151.0846|483=1741008634020|151=1900000.0|31=150.415|32=100000.0|132=151.067|133=151.079] [ttDemouk, 1, rdy] [i-6fomo-trading-Thread-1]\n" +
                "2025-03-03 10:00:30,371 [INFO ] OUT --> [35=8|34=287378|1=762933520|37=106936956|40=1|39=2|55=USDJPY|31=150.41500000000002|38=2000000.0|151=1850000.0|14=150000.0|54=2|59=4|60=1741014030371] [ttDemouk, 1, rdy] [i-6fomo-trading-Thread-1]\n" +
                "2025-03-03 10:00:30,500 [INFO ] OUT --> [35=AE|34=287380|1=762933520|568=cpos-262069ab2|855=2|39=2|55=USDJPY|17=10693695605|527=106936581|7501=62, Position closed partially|7401=-8898.338870431551|552=2|37=106936956|11=cp-1741014027502-137|44=150.586|40=1|60=1741014027503|54=2|38=2000000.0|800=2000000.0|14=2000000.0|4=1|6=151.0846|483=1741008634020|151=4000000.0|31=151.0846|32=2000000.0|132=151.067|133=151.079|552=2|37=106936956|11=cp-1741014027502-137|44=150.586|40=1|60=1741014030425|54=2|38=2000000.0|800=2000000.0|14=150000.0|4=2|6=150.41500000000002|483=1741014030425|151=1850000.0|31=150.415|32=50000.0|132=150.586|133=150.604] [ttDemouk, 1, rdy] [i-6fomo-trading-Thread-1]\n" +
                "2025-03-03 10:00:33,188 [INFO ] OUT --> [35=AE|34=287402|1=762933520|568=cpos-262069ab2|855=1|39=2|55=USDJPY|17=10693695601|527=106936581|7401=-667.14|552=1|37=106936956|11=cp-1741014027502-137|44=150.586|40=1|60=1741014027503|54=2|38=2000000.0|800=2000000.0|14=1850000.0|4=1|6=151.0846|483=1741008634020|151=1850000.0|31=150.415|32=50000.0|132=151.067|133=151.079] [ttDemouk, 1, rdy] [i-6fomo-trading-Thread-1]\n" +
                "2025-03-03 10:00:33,218 [INFO ] OUT --> [35=8|34=287405|1=762933520|37=106936956|40=1|39=2|55=USDJPY|31=150.41300000000004|38=2000000.0|151=1600000.0|14=400000.0|54=2|59=4|60=1741014033217] [ttDemouk, 1, rdy] [i-6fomo-trading-Thread-1]\n" +
                "2025-03-03 10:00:33,353 [INFO ] OUT --> [35=AE|34=287407|1=762933520|568=cpos-262069ab2|855=2|39=2|55=USDJPY|17=10693695605|527=106936581|7501=62, Position closed partially|7401=-8911.627906976466|552=2|37=106936956|11=cp-1741014027502-137|44=150.586|40=1|60=1741014027503|54=2|38=2000000.0|800=2000000.0|14=2000000.0|4=1|6=151.0846|483=1741008634020|151=4000000.0|31=151.0846|32=2000000.0|132=151.067|133=151.079|552=2|37=106936956|11=cp-1741014027502-137|44=150.586|40=1|60=1741014033264|54=2|38=2000000.0|800=2000000.0|14=400000.0|4=2|6=150.41400000000002|483=1741014033264|151=1600000.0|31=150.413|32=250000.0|132=150.586|133=150.604] [ttDemouk, 1, rdy] [i-6fomo-trading-Thread-1]\n" +
                "2025-03-03 10:00:35,927 [INFO ] OUT --> [35=AE|34=287491|1=762933520|568=cpos-262069ab2|855=2|39=2|55=USDJPY|17=10693695605|527=106936581|7501=62, Position closed partially|7401=-8924.916943521|552=2|37=106936956|11=cp-1741014027502-137|44=150.586|40=1|60=1741014027503|54=2|38=2000000.0|800=2000000.0|14=2000000.0|4=1|6=151.0846|483=1741008634020|151=4000000.0|31=151.0846|32=2000000.0|132=151.067|133=151.079|552=2|37=106936956|11=cp-1741014027502-137|44=150.586|40=1|60=1741014035860|54=2|38=2000000.0|800=2000000.0|14=900000.0|4=2|6=150.41300000000004|483=1741014035860|151=1100000.0|31=150.412|32=500000.0|132=150.586|133=150.604] [ttDemouk, 1, rdy] [i-6fomo-trading-Thread-1]\n" +
                "2025-03-03 10:00:38,519 [INFO ] OUT --> [35=AE|34=287502|1=762933520|568=cpos-262069ab2|855=1|39=2|55=USDJPY|17=10693695603|527=106936581|7401=-4017.66|552=1|37=106936956|11=cp-1741014027502-137|44=150.586|40=1|60=1741014027503|54=2|38=2000000.0|800=2000000.0|14=1100000.0|4=1|6=151.0846|483=1741008634020|151=1100000.0|31=150.412|32=500000.0|132=151.067|133=151.079] [ttDemouk, 1, rdy] [i-6fomo-trading-Thread-1]\n";
      //  String ll = "[ala=bala|ddd=fff]";
        String ll1 = "[8=FIX.4.2|9=94|34=1|35=A|49=MDTechForex1|52=20131122-20:55:18.539|56=Fastmatch1|98=010|8=30|141=Y|554=TFXUAT|10=254]";
        String line = "2025-02-13 01:38:12,752 INFO [i-0TradesExecutor-Thread-1] rfp-fix - OUT --> [35=AE|34=154|1=1594742|568=opos-6177f|855=1|39=11|55=USDJPY|17=90276239400|527=902762394|7402=4.0E-4|552=1|37=902762394|11=o-1739348386202-7|44=153.611|40=2|60=1739348386205|54=1|38=1000.0|800=1000.0|14=1000.0|4=1|6=153.613|483=1739348597428|31=153.613|32=1000.0|132=153.598|133=153.611] [ttdemoNY, 10, rdy]";
        String many = "2025/02/25 08:48:43.462 IN --> [35=8|34=49|1=1594742|37=107102120|40=1|39=3|55=USDJPY|31=149.42300000000003|38=1000.0|14=1000.0|54=2|59=4|60=1740491323459] Thread ID: 8933\n" +
                "2025/02/25 08:48:43.539 IN --> [35=AE|34=50|1=1594742|568=cpos-8823c|855=2|39=3|55=USDJPY|17=10710212000|527=107102118|7401=-0.32205609055572|552=2|37=107102118|11=p-1740491238115-5|44=149.484|40=1|60=1740491238164|54=1|38=1000.0|800=1000.0|14=1000.0|4=1|6=149.484|483=1740491238514|31=149.484|32=1000.0|132=149.471|133=149.482|552=2|37=107102120|11=cp-1740491322169-7|44=149.418|40=1|60=1740491323509|54=2|38=1000.0|800=1000.0|14=1000.0|4=2|6=149.42300000000003|483=1740491323509|31=149.425|32=1000.0|132=149.423|133=149.434] Thread ID: 8933\n" +
                "2025/02/25 08:48:43.539 Manage comment for closed OrderID [107102118]. New: [], current [null], remove it = [false]? Thread ID: 165\n" +
                "2025/02/25 08:48:43.539 After manage comment for closed Order = [VTTrade [providerAcountId=1594742, orderId=107102118, secondaryExecID=107102118, tradeId=10710211800, positionId=107102118, clientOrderId=p-1740491238115-5, positionType=LIMIT, positionState=OPEN, positionSide=BUY, feed=VTFeed, code=USDJPY, time=1740491238514, price=149.484, openBidPrice=149.471, openAskPrice=149.482, closeBidPrice=0.0, closeAskPrice=0.0, units=1000.0, takeProfit=NaN, stopLoss=NaN, trailingStopOffset=NaN, pl=0.0, commission=0.0, interest=0.0, dividend=0.0, closeTime=0, closePrice=0.0, expiration=0, orderIdStopLoss=null, orderIdTakeProfit=null, closeOrderId=null, comment=null]] Thread ID: 165\n" +
                "2025/02/25 08:48:43.542 Updating existing closed trade with TradeID = 10710212000, OrderID = 107102118, account = 423624707 Thread ID: 1735\n" +
                "2025/02/25 08:48:43.549 Stored [true] a TradePN for acct [1594742] and VTTrade [107102118, 10710212000] Thread ID: 1735\n" +
                "2025/02/25 08:48:43.549 Stored closed trade, from MsqSeqNo[50], trade: VTTrade [providerAcountId=1594742, orderId=107102118, secondaryExecID=107102118, tradeId=10710212000, positionId=107102118, clientOrderId=p-1740491238115-5, positionType=LIMIT, positionState=CLOSED, positionSide=BUY, feed=VTFeed, code=USDJPY, time=1740491238514, price=149.484, openBidPrice=149.471, openAskPrice=149.482, closeBidPrice=149.423, closeAskPrice=149.434, units=1000.0, takeProfit=NaN, stopLoss=NaN, trailingStopOffset=NaN, pl=-0.32205609055572, commission=0.0, interest=0.0, dividend=0.0, closeTime=1740491323509, closePrice=149.42300000000003, expiration=0, orderIdStopLoss=null, orderIdTakeProfit=null, closeOrderId=null, comment=null] Thread ID: 1735\n" +
                "2025/02/25 08:49:07.338 IN --> [35=BA|34=51|1=1594742|909=rCol-6823c|7205=1|910=4|7201=1|15=GBP|899=10270.61|900=10270.61|901=10270.61|7602=100|7207=1.0] Thread ID: 8933\n" +
                "2025/02/25 08:49:07.338 On Collateral for TT account: VTAccount [tradingAccountId=423624707, providerId=4194304, providerAccountId=1594742, accountType=DEMO, baseCurrency=GBP, quantityType=UNITS, balance=10270.61, equity=10270.61, usedMargin=0.0, freeMargin=10270.61, leverage=0, enabled=true, accountStatus=TRADING, lotMultiplier=0.0, marginCalculationType=0, unitId=100, accountMarketType=CFD, marginCallLevel=1.0] Thread ID: 165\n" +
                "2025/02/25 08:50:10.948 Requesting Instruments and initial quotes... Thread ID: 96\n" +
                "2025/02/25 08:50:10.948 Executing GET [https://tt-dev1.thinkmarkets.com/portaladmin/rest/instruments_rfp] Thread ID: 96\n" +
                "2025/02/25 08:50:11.000 DONE requesting Instruments and initial quotes... Thread ID: 96\n" +
                "2025/02/25 08:50:11.000\n" +
                " Trading Adapter received VTSymbolInfo-s with size = 111, XML configured size = 3082\n" +
                " Thread ID: 96\n" +
                "2025/02/25 08:50:11.002 Requesting margin requirements for instruments... Thread ID: 96\n" +
                "2025/02/25 08:50:11.002 Executing GET [https://tt-dev1.thinkmarkets.com/portaladmin/rest/unitmarginrequirements] Thread ID: 96\n" +
                "2025/02/25 08:50:11.021 DONE requesting margin requirements for instruments... Thread ID: 96\n" +
                "2025/02/25 08:51:14.317 Client request: OrderCreateRequest [reqId=null, providerAccountId=1594742, instrument=USDSEK, units=2000000, side=BUY, type=LIMIT, expiration=0, price=null, desiredOpenPrice=10.60630, sl:\n"+
                "2025/02/25 08:51:15.317 ]";


        FixDictionary dict = new FixDictionary();
        dict.init();

        LogProcessor pr = new LogProcessor(dict);
        String [] ll = splipagelog.split("\n");
        for (String s: ll)
            pr.processLine(s);
        System.out.println("\n"+pr.getMsg());

    }
}
