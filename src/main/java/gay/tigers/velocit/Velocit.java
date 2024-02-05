package gay.tigers.velocit;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import gay.tigers.velocit.configuration.Config;
import gg.playit.api.ApiClient;
import gg.playit.api.actions.CreateTunnel;
import gg.playit.api.models.*;
import gg.playit.control.PlayitControlChannel;
import gg.playit.minecraft.PlayitConnectionTracker;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

@Plugin(
        id = "velocit",
        name = "velocit",
        version = "1.0-pre"
)
public class Velocit {
    final PlayitKeys keys = new PlayitKeys();
    Config Config;

    public final ApiClient unauthenticatedApiClient = new ApiClient(null);
    private ApiClient playitApiClient;
    PlayitControlChannel playitControlChannel;
    final PlayitConnectionTracker playitConnectionTracker = new PlayitConnectionTracker();
    VelocityServer server;
    final ProxyServer proxy;
    final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    @Inject
    Logger logger;

    @Inject
    public Velocit(ProxyServer proxy) {
        this.server = (VelocityServer) proxy;
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
        createTunnel.localIp = address.getAddress().getHostAddress();
        createTunnel.localPort = address.getPort();
        createTunnel.agentId = keys.agentId;
        createTunnel.portCount = 1;
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
            logger.error("Failed to create tunnel! " + e);
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
        Thread t = new Thread(new StatusRunnable(this));
        t.start();
        if(Objects.equals(Config.Current.PlayitSecret, "")){
            logger.warn("No PlayIt secret set!");
            StatusRunnable.status.set(1);
            return;
        }
        keys.createdSecret = Config.Current.PlayitSecret;
        StatusRunnable.status.set(2);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event){
        StatusRunnable.status.set(-1);
        if(playitControlChannel == null) return;
        try {
            playitControlChannel.close();
            logger.info("Shutdown channel. Goodbye!");
        } catch (IOException e) {
            logger.error("Failed to shutdown channel! " + e);
        }
    }
}
