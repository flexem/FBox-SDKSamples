import fbox.ConsoleLoggerFactory;
import fbox.ServerCaller;
import fbox.StaticCredentialProvider;
import fbox.TokenManager;
import fbox.models.*;

import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        ConsoleLoggerFactory loggerFactory = new ConsoleLoggerFactory();

        // 指定连接服务器的凭据参数
        TokenManager tokenManager = new TokenManager(new StaticCredentialProvider(Global.clientId, Global.clientSecret, Global.username, Global.password), Global.idServerUrl, loggerFactory);

        ServerCaller commServer = new ServerCaller(tokenManager, Global.commServerApiUrl, Global.signalrClientId, loggerFactory);
        ServerCaller appServer = new ServerCaller(tokenManager, Global.appServerApiUrl, Global.signalrClientId, loggerFactory);
        ServerCaller hdataServer = new ServerCaller(tokenManager, Global.hdataServerApiUrl, Global.signalrClientId, loggerFactory);

        Global.commServer = commServer;
        Global.appServer = appServer;
        Global.hdataServer = hdataServer;

        FBoxSignalRConnection fboxSignalR = new FBoxSignalRConnection(Global.commServerSignalRUrl, Global.signalrClientId, tokenManager, Global.proxy, loggerFactory);

        // 连接SignalR推送通道
        fboxSignalR.start();

        System.out.println("Box list:");
        try {
            // 读取盒子列表
            BoxGroup[] boxGroups = Global.appServer.executeGet("box/grouped", BoxGroup[].class);
//            long boxId1 = 0;
            // 返回的是 盒子分组-盒子注册项（BoxReg） 的二层结构
            for (BoxGroup group : boxGroups) {
                for (BoxReg boxReg : group.boxRegs) {
                    System.out.printf("\t%s\t%s\t%s\n", boxReg.alias, boxReg.box.boxNo, boxReg.box.boxType);
                }
            }

//            //监控点写值
//            Global.commServer.executePost(String.format("api/v2/box/dmon/value?boxNo=%s", "300012345678（盒子序列号）"),
//                    new DmonWriteValueArgs("组名", "监控点名", "12.34"), null);
//
//

            String boxNo = "300015050009";
            // 获取历史数据定义
            HdataDef[] hdataDefs = Global.commServer.executeGet(String.format("v2/hdataitems?boxNo=%s", boxNo), HdataDef[].class);

            // 获取这个盒子上所有的历史数据通道
            List<HdataChannelDef> channels = Arrays.stream(hdataDefs).flatMap(x -> Arrays.stream(x.channels)).collect(Collectors.toList());
            // 通道ID列表
            List<Long> channelIds = channels.stream().map(c -> c.uid).collect(Collectors.toList());
            List<String> channelNames = channels.stream().map(c -> c.name).collect(Collectors.toList());

            // 获取按通道的数据 （数组第1维是固定两个元素时间和值，第2维是单个通道的所有数据，第3维是请求的每个通道）
            Object[][][] result =
//                    String  result =
                    Global.hdataServer.executePost(String.format("v2/hdata/get"),
                            new GetByChannelHdataArgs(channelIds, new Date().getTime() - 7 * 86400000, new Date().getTime(), -100, 3),
//                    String.class);
                            Object[][][].class);

            System.out.println(result);

            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");

            for (int i = 0; i < result.length; i++) {
                Object[][] channel = result[i];
                System.out.print("Channel " + channelNames.get(i));
                for (Object[] datum : channel) {
                    System.out.printf("(%s: %s)", dateFormatter.format(new Date(Math.round((double) datum[0]))), datum[1]);
                }
                System.out.println();
            }

            //获取按行的数据（每行固定有通道个数个数据，如果这行的时间某些通道没有值，则为null）
            ByRowHdata result2 =
//            String result2 =
                    Global.hdataServer.executePost(String.format("v2/hdata/get"),
                            new GetByRowHdataArgs(channelIds, new Date().getTime() - 7 * 86400000, new Date().getTime(), -100, 3),
//                            String.class);
                            ByRowHdata.class);
//            System.out.println(result2);

            for (ByRowHdataRow row : result2.rows) {
                System.out.print(dateFormatter.format(row.getTime()) + ": ");
                Object[] c = row.c;
                for (int i = 0; i < c.length; i++) {
                    Object value = c[i];
                    if (i == 0) {
                        System.out.printf("%s", value);
                    } else {
                        System.out.printf(",%s", value);
                    }
                }
                System.out.println();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }


        Scanner s = new Scanner(System.in);
        s.nextLine();
    }
}
