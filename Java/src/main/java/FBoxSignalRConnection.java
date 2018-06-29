import com.github.signalr4j.client.hubs.HubProxy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fbox.LoggerFactory;
import fbox.TokenManager;
import fbox.models.BoxStateChanged;
import fbox.signalr.SignalRConnectionBase;

import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class FBoxSignalRConnection extends SignalRConnectionBase {
    private final Gson gson;
    private final fbox.Logger logger;
    ConcurrentHashMap<Long, LongAdder> dmonIds = new ConcurrentHashMap<>();
    private LongAdder dmonMsgCounter = new LongAdder();
    private long lastDmonItemCount;
    private long lastDmonMsgCount;
    private long lastReportTime;
    private Proxy proxy;
    private LongAdder dmonItemCounter = new LongAdder();

    public FBoxSignalRConnection(String hubUrl, String signalrClientId, TokenManager tokenManager, Proxy proxy, LoggerFactory loggerFactory) {
        super(hubUrl, signalrClientId, tokenManager, proxy, loggerFactory);
        this.logger = loggerFactory.createLogger("FBoxSignalRConnection");
        this.proxy = proxy;
        gson = new GsonBuilder().create();
        new Thread(() -> {
            for (; ; ) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long currentTime = System.nanoTime();
                long currentMsgCount = this.dmonMsgCounter.longValue();
                long currentItemCount = this.dmonItemCounter.longValue();
                long msgRate = (currentMsgCount - this.lastDmonMsgCount) * 1000000000 / (currentTime - this.lastReportTime);
                long itemRate = (currentItemCount - this.lastDmonItemCount) * 1000000000 / (currentTime - this.lastReportTime);
                this.logger.logInformation(String.format("Dmon id count: %d, item rate: %d, message rate: %d", this.dmonIds.size(), itemRate, msgRate));
                this.lastReportTime = currentTime;
                this.lastDmonMsgCount = currentMsgCount;
                this.lastDmonItemCount = currentItemCount;
            }
        }).start();
    }

    @Override
    public void connected() {
        super.connected();
        dmonIds.clear();
    }


    protected void onHubProxyDestroyed(HubProxy hubProxy){
        hubProxy.removeSubscription("dmonUpdateValue");
        hubProxy.removeSubscription("alarmTriggered");
        hubProxy.removeSubscription("alarmRecovered");
        hubProxy.removeSubscription("boxConnStateChanged");
    }

    @Override
    protected void onHubProxyCreated(HubProxy hubProxy) {
        hubProxy.subscribe("dmonUpdateValue").addReceivedHandler(jsonElements -> {
            Global.threadPool.submit(() -> {
                this.dmonMsgCounter.increment();
//                System.out.println("Dmon data received: ");
                JsonArray items = jsonElements[1].getAsJsonArray();
                for (com.google.gson.JsonElement jsonElement : items) {
                    JsonObject item = jsonElement.getAsJsonObject();
                    this.dmonIds.computeIfAbsent(item.get("id").getAsLong(), aLong -> new LongAdder()).increment();
                    this.dmonItemCounter.increment();
                    //收到的推送数据
                    String name = item.get("name").getAsString();
                    String value = item.get("value").getAsString();
//                    System.out.printf("%s:%s ", name, value);
                }
            });
        });

        hubProxy.subscribe("alarmTriggered").addReceivedHandler(jsonElements -> {
            Global.threadPool.submit(() -> {
                System.out.println("Alarm triggered: ");
                for (com.google.gson.JsonElement jsonElement : jsonElements) {
                    System.out.println("\t" + jsonElement);
                }
            });
        });

        hubProxy.subscribe("alarmRecovered").addReceivedHandler(jsonElements -> {
            Global.threadPool.submit(() -> {
                System.out.println("Alarm recovered: ");
                for (com.google.gson.JsonElement jsonElement : jsonElements) {
                    System.out.println("\t" + jsonElement);
                }
            });
        });

        hubProxy.subscribe("boxConnStateChanged").addReceivedHandler(jsonElements -> {
            Global.threadPool.submit(() -> {
                System.out.println("Box state changed.");
                if (jsonElements.length <= 0)
                    return;
                BoxStateChanged[] stateChanges = gson.fromJson(jsonElements[0], BoxStateChanged[].class);
                for (BoxStateChanged stateChange : stateChanges) {
                    // stateChange.id 是盒子列表中BoxReg对象下的box.id，可以根据这个过滤要开的盒子。
                    // stateChange.state 为1是盒子上线事件。
                    if (stateChange.state == 1) {
                        try {
                            Global.commServer.executePost("box/" + stateChange.id + "/dmon/start", String.class);
                            System.out.println("Start dmon points on box " + stateChange.id + " ok.");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });
//            }

//            Global.threadPool.submit(() -> {
//                try {
//                    BoxGroup[] boxGroups = Global.appServer.executeGet("box/grouped", BoxGroup[].class);
//
//                    for (BoxGroup group : boxGroups) {
//                        for (BoxReg boxReg : group.boxRegs) {
//                            String boxNo = boxReg.box.boxNo;
//                            Global.commServer.executePost("dmon/start?boxNo=" + boxNo, String.class);
//                            System.out.println("Start dmon points on box " + boxNo + " ok.");
//                        }
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });
    }
}
