package chat.amy.shard;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author amy
 * @since 10/1/17.
 */
@SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
public class RancherSharder implements Sharder {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build();
    
    @Override
    public int getShardId() {
        try {
            final String serviceIndex = client.newCall(new Request.Builder()
                    .url("http://rancher-metadata/2015-12-19/self/container/service_index")
                    .build()).execute().body().string();
            return Integer.parseInt(serviceIndex) - 1;
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public int getShardScale() {
        try {
            final String serviceName = client.newCall(new Request.Builder()
                    .url("http://rancher-metadata/2015-12-19/self/container/service_name")
                    .build()).execute().body().string();
            final String serviceScale = client.newCall(new Request.Builder()
                    .url(String.format("http://rancher-metadata/2015-12-19/services/%s/scale", serviceName))
                    .build()).execute().body().string();
            return Integer.parseInt(serviceScale);
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
