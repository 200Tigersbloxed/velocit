package gay.tigers.velocit.configuration;

import me.grison.jtoml.impl.Toml;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    private static final String CONFIGURATION_LOCATION = "config/gay.tigers.velocit/config.toml";

    public String PlayitSecret;

    public Config(Logger logger){
        File file = new File(CONFIGURATION_LOCATION);
        if(!file.exists()){
            String s = Toml.serialize(this);
            try {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(s);
                fileWriter.close();
            } catch (IOException e) {
                logger.error("Cannot write default config to file!");
            }
            return;
        }
        Toml t = Toml.parse(file);
        PlayitSecret = t.getString("PlayitSecret");
    }
}
