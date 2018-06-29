package fbox.signalr;

import com.github.signalr4j.client.LogLevel;
import com.github.signalr4j.client.Logger;

public class SignalRLoggerWrapper implements Logger {

    private final fbox.Logger logger;

    public SignalRLoggerWrapper(fbox.Logger logger){
        this.logger = logger;
    }
    @Override
    public void log(String message, LogLevel level) {
        switch (level) {
            case Critical:
                this.logger.logError(message);
                break;
            case Information:
                this.logger.logInformation(message);
                break;
            case Verbose:
                this.logger.logTrace(message);
                break;
        }
    }

}
