package gg.playit.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Protocol {
    @JsonProperty
    public String protocol;

    @JsonProperty
    public String local_ip;

    @JsonProperty
    public int local_port;

    @JsonProperty
    public int agent_id;
}
