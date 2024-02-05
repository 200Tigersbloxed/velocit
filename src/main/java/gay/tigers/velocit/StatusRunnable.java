package gay.tigers.velocit;

import gg.playit.control.PlayitControlChannel;
import gg.playit.messages.ControlFeedReader;
import gg.playit.minecraft.utils.Hex;

import java.io.IOException;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

class StatusRunnable implements Runnable{
    public static AtomicInteger status = new AtomicInteger(0);

    private final Velocit velocit;
    private boolean cancel;

    private String hex;

    StatusRunnable(Velocit velocit){ this.velocit = velocit; }

    @Override
    public void run() {
        while(!cancel) {
            switch (status.get()){
                case 0:
                    break;
                case 1:
                    // No Secret found, create one
                    if(hex == null){
                        byte[] array = new byte[8];
                        new Random().nextBytes(array);
                        hex = Hex.encodeHexString(array);
                    }
                    velocit.keys.createdSecret = velocit.unauthenticatedApiClient.exchangeClaimForSecret(hex);
                    if(velocit.keys.createdSecret == null){
                        velocit.logger.info("Claim this session at https://playit.gg/mc/" + hex);
                    }
                    else{
                        // Got key, move on
                        status.set(2);
                    }
                    break;
                case 2:
                    // Verify the secret
                    velocit.Start();
                    break;
                case 3:
                    // Create agent
                    velocit.Connect();
                    break;
                case 4:
                    try {
                        Optional<ControlFeedReader.ControlFeed> option = velocit.playitControlChannel.update();
                        if(!option.isPresent()) break;
                        ControlFeedReader.ControlFeed feed = option.get();
                        if(feed instanceof ControlFeedReader.NewClient newClient){
                            String clientKey = newClient.peerAddr + "-" + newClient.connectAddr;
                            if(velocit.playitConnectionTracker.addConnection(clientKey)){
                                // TODO: Create TCPListener and deregister connection from connection tracker
                            }
                        }
                    } catch (IOException e) {
                        velocit.logger.error(e.toString());
                    }
                    break;
                default:
                    cancel = true;
                    break;
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}
        }
    }
}
