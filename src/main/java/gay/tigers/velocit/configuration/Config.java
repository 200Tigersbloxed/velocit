package gay.tigers.velocit.configuration;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    private static final String DIR_LOCATION = "plugins/velocit";
    private static final String CONFIGURATION_LOCATION = DIR_LOCATION + "/config.toml";

    private final Logger logger;

    public VelocitConfig Current;

    public Config(Logger logger){
        this.logger = logger;
        File file = new File(CONFIGURATION_LOCATION);
        if(!file.exists()){
            new File(DIR_LOCATION).mkdirs();
            Current = new VelocitConfig();
            Save();
            return;
        }
        Toml toml = new Toml().read(file);
        Current = toml.to(VelocitConfig.class);
    }

    public void Save(){
        File file = new File(CONFIGURATION_LOCATION);
        TomlWriter tomlWriter = new TomlWriter();
        String s = tomlWriter.write(Current);
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(s);
            logger.info("Saving: " + s);
            fileWriter.close();
        } catch (IOException e) {
            logger.error("Cannot write default config to file!");
        }
    }
}
