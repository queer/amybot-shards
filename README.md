# amybot shards

The magical thing that dumps Discord events into the backend. :tm:

## Configuration

The following environment variables are used. Defaults are shown here.

```bash
# The token for your bot
BOT_TOKEN="no default provided, obviously"
# The URL of your redis host. Currently can only be used in single-node mode, but I want to add cluster support eventually
REDIS_HOST="redis://redis:6379"
REDIS_PASS="dank memes"
```