package gay.tigers.velocit;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import gay.tigers.velocit.configuration.Config;
import gg.playit.api.ApiClient;
import gg.playit.api.actions.CreateTunnel;
import gg.playit.api.models.*;
import gg.playit.control.PlayitControlChannel;
import gg.playit.minecraft.PlayitConnectionTracker;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

@Plugin(
        id = "velocit",
        name = "velocit",
        version = "1.0-SNAPSHOT"
)
public class Velocit {
    final PlayitKeys keys = new PlayitKeys();
    Config Config;

    public final ApiClient unauthenticatedApiClient = new ApiClient(null);
    private ApiClient playitApiClient;
    PlayitControlChannel playitControlChannel;
    final PlayitConnectionTracker playitConnectionTracker = new PlayitConnectionTracker();


    private final ProxyServer proxy;
    @Inject
    Logger logger;

    @Inject
    public Velocit(ProxyServer proxy) {
        this.proxy = proxy;
    }

    private CreateTunnel createTunnel(){
        InetSocketAddress address = proxy.getBoundAddress();
        CreateTunnel createTunnel = new CreateTunnel();
        createTunnel.tunnelType = TunnelType.MinecraftJava;
        if(Config.Current.ForwardBoth)
            createTunnel.portType = PortType.BOTH;
        else
            createTunnel.portType = PortType.TCP;
        createTunnel.localIp = address.getAddress().toString();
        createTunnel.localPort = address.getPort();
        createTunnel.agentId = keys.agentId;
        return createTunnel;
    }

    void Start(){
        Config.Current.PlayitSecret = keys.createdSecret;
        Config.Save();
        playitApiClient = new ApiClient(keys.createdSecret);
        try {
            // Get status
            SessionStatus sessionStatus = playitApiClient.getStatus();
            keys.agentId = sessionStatus.agentId;
            // Find/Create Tunnel
            boolean found = false;
            for (AccountTunnel tunnel : playitApiClient.listTunnels().tunnels){
                if(tunnel.tunnelType != TunnelType.MinecraftJava) continue;
                found = true;
            }
            if(!found) playitApiClient.createTunnel(createTunnel());
            StatusRunnable.status.set(3);
        } catch (IOException e) {
            logger.error(e.toString());
        }
    }

    void Connect(){
        // Create Channel
        try {
            playitControlChannel = PlayitControlChannel.setup(keys.createdSecret);
            StatusRunnable.status.set(4);
        } catch (IOException e) {
            logger.error("Failed to setup channel! " + e);
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Config = new Config(logger);
        new Thread(new StatusRunnable(this)).start();
        if(Objects.equals(Config.Current.PlayitSecret, "")){
            logger.warn("No PlayIt secret set!");
            StatusRunnable.status.set(1);
            return;
        }
        keys.createdSecret = Config.Current.PlayitSecret;
        StatusRunnable.status.set(2);
    }
}
