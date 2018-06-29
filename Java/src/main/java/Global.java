import fbox.ServerCaller;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Global {
    public static Proxy proxy = null;
    public static ExecutorService threadPool = Executors.newCachedThreadPool();
    public static ServerCaller commServer;
    public static ServerCaller appServer;
    public static ServerCaller hdataServer;

// 以下服务器地址是繁易公有云，私有云请根据实际情况修改
    public static final String idServerUrl = "https://account.flexem.com/core/";
    public static String appServerApiUrl = "http://fbox360.com/api/client/";
    public static final String commServerApiUrl = "http://fbcs101.fbox360.com/api/";
    public static final String commServerSignalRUrl = "http://fbcs101.fbox360.com/push";
    public static String hdataServerApiUrl = "http://fbhs1.fbox360.com/api/";
    public static String signalrClientId = UUID.randomUUID().toString();

    public static String username = "用户名";
    public static String password = "密码";
    // 获取API账号请咨询商务。
    public static String clientId = "API账号";
    public static String clientSecret = "API密钥";
}
