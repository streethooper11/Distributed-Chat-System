# Distributed-Chat-System
A school project that can send and receive messages with other peers in command line.

# How to use
Registry is the distributing server which will need to be run first.\
Then P3_opt could be run with arguments

# Arguments for Registry
port number may be given as the 1st argument. Port number defaults to 55921 otherwise.

# Arguments for P3_opt
P3_opt has command-line flags:
--ip IP_OF_REGISTRY
--port PORT_NUMBER
--name NAME_OF_CLIENT
--v VERBOSE_FLAG
--psend PEER_MSG_SEND_INTERVAL_IN_SECONDS
--alive PEER_KEEPALIVE_DURATION_IN_SECONDS
