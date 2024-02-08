# velocit
A [playit.gg](https://playit.gg) plugin for [Velocity](https://github.com/PaperMC/Velocity) servers

# Getting Started

1. [Setup a Velocity Server](https://docs.papermc.io/velocity/getting-started)
2. Install the plugin's [latest release](https://github.com/200Tigersbloxed/velocit/releases/latest)
3. Click the link in the console window to link your playit account to the server
4. Done!

# What's supported?

Pretty much anything that's TCP will work right now. Only one TCP port is forwarded; however, this can be shaped in the future.

Feature | Description | Supported
--- | --- | ---
Minecraft TCP | The TCP port for Velocity | ✔️
UDP Ports | UDP Ports (for things like Geyser) | ❌
Extra TCP Ports | Any extra TCP ports to expose | ✔️
IP Forwarding | The actual IP address of the client | ❌

*any feature listed is planned*

# How to Build?

1. Pull the repo
2. Open with your favorite Java IDE
3. Refresh maven packages
4. `mvn install`
