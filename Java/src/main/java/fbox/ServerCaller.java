package fbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.http.*;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.util.EntityUtils;

import javax.lang.model.element.VariableElement;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

public class ServerCaller {
    private static final long RETRY_SLEEP_TIME_MILLIS = 2000;
    private static ArrayList<Class<?>> exceptionWhitelist = new ArrayList<>();
    private static ArrayList<Class<?>> exceptionBlacklist = new ArrayList<>();

    static {
        // Retry if the server dropped connection on us
        exceptionWhitelist.add(NoHttpResponseException.class);
        // retry-this, since it may happens as part of a Wi-Fi to 3G failover
        exceptionWhitelist.add(UnknownHostException.class);
        // retry-this, since it may happens as part of a Wi-Fi to 3G failover
        exceptionWhitelist.add(SocketException.class);

        // never retry timeouts
        exceptionBlacklist.add(InterruptedIOException.class);
        // never retry SSL handshake failures
        exceptionBlacklist.add(SSLException.class);
    }

    private final String baseUrl;
    private final String signalrClientId;
    private final Gson gson;
    private final Logger logger;
    private CloseableHttpClient http;
    private TokenManager tokenManager;
    private String accessToken;
    private int maxRetries = 3;

    public ServerCaller(TokenManager tokenManager, LoggerFactory loggerFactory) throws MalformedURLException {
        this(tokenManager, null, null, loggerFactory);
    }

    public ServerCaller(TokenManager tokenManager, String signalrClientId, LoggerFactory loggerFactory) {
        this(tokenManager, null, signalrClientId, null, null, loggerFactory);
    }

    public ServerCaller(TokenManager tokenManager, String baseUrl, String signalrClientId, LoggerFactory loggerFactory) {
        this(tokenManager, baseUrl, signalrClientId, null, null, loggerFactory);
    }

    public ServerCaller(TokenManager tokenManager, String baseUrl, String signalrClientId, CloseableHttpClient http, HttpHost proxy, LoggerFactory loggerFactory) {
        logger = loggerFactory.createLogger("ServerCaller");
        this.baseUrl = baseUrl;
        this.tokenManager = tokenManager;
        this.signalrClientId = signalrClientId;
        this.http = http;

        this.gson = new GsonBuilder().create();

        ArrayList<Header> headers = new ArrayList<>();
        if (signalrClientId != null) {
            headers.add(new BasicHeader("X-FBox-ClientId", signalrClientId));
        }

        if (http == null) {
            HttpClientBuilder httpBuilder = HttpClients.custom()
                    .setDefaultHeaders(headers)
                    .setRetryHandler((exception, executionCount, context) -> {
                        boolean retry = true;

                        Boolean b = (Boolean) context.getAttribute(ExecutionContext.HTTP_REQ_SENT);
                        boolean sent = (b != null && b.booleanValue());

                        if (executionCount > maxRetries) {
                            // Do not retry if over max retry count
                            retry = false;
                        } else if (isInList(exceptionBlacklist, exception)) {
                            // immediately cancel retry if the error is blacklisted
                            retry = false;
                        } else if (isInList(exceptionWhitelist, exception)) {
                            // immediately retry if error is whitelisted
                            retry = true;
                        } else if (!sent) {
                            // for most other errors, retry only if request hasn't been fully
                            // sent yet
                            retry = true;
                        }

                        HttpClientContext clientContext = HttpClientContext.adapt(context);
                        HttpRequest request = clientContext.getRequest();
                        HttpResponse response = clientContext.getResponse();
                        StatusLine statusLine = response.getStatusLine();

                        boolean unauthorized = false;
                        if (statusLine != null && statusLine.getStatusCode() == 401) {
                            unauthorized = true;
                            ServerCaller caller = (ServerCaller) context.getAttribute("FBoxServerCaller");
                            try {
                                System.out.println("ServerCaller: try get another token for " + request.toString());
                                caller.accessToken = caller.tokenManager.getOrUpdateToken(caller.accessToken);
                                request.setHeader("Authorization", "Bearer " + caller.accessToken);
                                retry = true;
                            } catch (IOException e) {
                                e.printStackTrace();
                                retry = false;
                            }
                        }

                        if (retry && !unauthorized) {
                            try {
                                Thread.sleep(RETRY_SLEEP_TIME_MILLIS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            exception.printStackTrace();
                        }
                        return retry;
                    });
            if (proxy != null)
                httpBuilder = httpBuilder.setProxy(proxy);
            this.http = httpBuilder.build();
        }
    }

    protected boolean isInList(ArrayList<Class<?>> list, Throwable error) {
        Iterator<Class<?>> itr = list.iterator();
        while (itr.hasNext()) {
            if (itr.next().isInstance(error)) {
                return true;
            }
        }
        return false;
    }

    public <T> T executeGet(String url, Class<T> responseType) throws IOException {
        HttpGet request;
        request = new HttpGet(baseUrl + url);
        return executeCore(request, responseType);
    }

    public <T> T executePost(String url, Class<T> responseType) throws IOException {
        return executePost(url, null, responseType);
    }

    public <T> T executePost(String url, Object entity, Class<T> responseType) throws IOException {
        HttpPost request;
        request = new HttpPost(baseUrl + url);
        if (entity != null) {
            String str = gson.toJson(entity);
            request.setEntity(new StringEntity(str, ContentType.APPLICATION_JSON));
        }
        return executeCore(request, responseType);
    }

    public <T> T executePost(String url, HttpEntity body, Class<T> responseType) throws IOException {
        HttpPost request;
        request = new HttpPost(baseUrl + url);
        if (body != null)
            request.setEntity(body);
        return executeCore(request, responseType);
    }

    public <T> T executePut(String url, HttpEntity body, Class<T> responseType) throws IOException {
        HttpPut request;
        request = new HttpPut(baseUrl + url);
        request.setEntity(body);
        return executeCore(request, responseType);
    }

    public <T> T executeDelete(String url, Class<T> responseType) throws IOException {
        HttpDelete request;
        request = new HttpDelete(baseUrl + url);
        return executeCore(request, responseType);
    }

    private <T> T executeCore(HttpUriRequest request, Class<T> responseType) throws IOException {
        if (this.accessToken == null)
            this.accessToken = this.tokenManager.getOrUpdateToken(this.accessToken);
        request.setHeader("Authorization", "Bearer " + this.accessToken);
        request.addHeader("Accept", "application/json; */*");
        for (; ; ) {
            String method = request.getMethod();
            URI uri = request.getURI();
            this.logger.logTrace(String.format("Executing request %s %s", method, uri));
            CloseableHttpResponse response = this.http.execute(request);
            try {
                StatusLine statusLine = response.getStatusLine();
                if (statusLine == null)
                    throw new HttpResponseException(0, "Null status line.");
                int statusCode = statusLine.getStatusCode();
                this.logger.logTrace(String.format("Executed request %s %s with code %d", method, uri, statusCode));
                if (statusCode == 401) {
                    this.logger.logTrace("ServerCaller: 401 from " + request.getURI().toString());
                    this.accessToken = this.tokenManager.getOrUpdateToken(this.accessToken);
                    request.setHeader("Authorization", "Bearer " + this.accessToken);
                    continue;
                } else if (statusCode >= 300) {
                    String exmsg = statusLine.getReasonPhrase();
                    Header errCodeHeader = response.getFirstHeader("X-FBox-Code");
                    int errCode = 0;
                    if (errCodeHeader != null) {
                        errCode = Integer.parseInt(errCodeHeader.getValue());
                        exmsg += " code=" + errCode;
                    }
                    throw new BoxServerResponseException(statusCode, exmsg, errCode);
                }
                HttpEntity body = response.getEntity();
                if (body != null && responseType != null) {
                    String str = EntityUtils.toString(body);
                    try {
                        return gson.fromJson(str, responseType);
                    } catch (JsonSyntaxException ex) {
                        if (responseType.isAssignableFrom(String.class))
                            return (T) str;
                        else
                            throw new IllegalArgumentException("Response cannot be parsed to " + responseType.toString() + " contentType is " + body.getContentType());
                    }
                }
//                this.logger.logTrace(String.format("Request %s %s returned with empty body.", method, uri));
                return null;
            } finally {
                response.close();
            }
        }
    }
}
