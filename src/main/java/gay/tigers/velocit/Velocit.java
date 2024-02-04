package gay.tigers.velocit;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import gay.tigers.velocit.configuration.Config;
import gg.playit.api.ApiClient;
import org.slf4j.Logger;

import java.util.Objects;

@Plugin(
        id = "velocit",
        name = "velocit",
        version = "1.0-SNAPSHOT"
)
public class Velocit {
    private Config Config;
    private ApiClient playitApiClient;

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Config = new Config(logger);
        if(Objects.equals(Config.PlayitSecret, "")){
            logger.warn("No PlayIt secret set!");
            return;
        }
        playitApiClient = new ApiClient(Config.PlayitSecret);
        // TODO: Create tunnel
    }
}
