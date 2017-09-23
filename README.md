# amybot shards

The magical thing that dumps Discord events into the backend. :tm:

Due to some needs of mine, this is also a Discord API library, of sorts. Eventually I'll be able to replace JDA, but not any time soon

## Configuration

The following environment variables are used. Defaults are shown here.

```bash
# The token for your bot
BOT_TOKEN="no default provided, obviously"
# The URL of your redis host. Currently can only be used in single-node mode, but I want to add cluster support eventually
REDIS_HOST="redis://redis:6379"
REDIS_PASS="dank memes"
```