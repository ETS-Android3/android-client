package io.split.android.client.impressions;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpResponse;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.Utils;

public class HttpImpressionsSender implements ImpressionsSender {

    private HttpClient _client;
    private URI _eventsEndpoint;
    private ImpressionsStorageManager _storageManager;

    public HttpImpressionsSender(HttpClient client, String eventsEndpoint, ImpressionsStorageManager impressionsStorageManager) throws URISyntaxException {
        _client = client;
        _eventsEndpoint = new URIBuilder(eventsEndpoint).setPath("/api/testImpressions/bulk").build();
        _storageManager = impressionsStorageManager;
    }

    @Override
    public boolean post(List<TestImpressions> impressions) {
        if (impressions == null || impressions.isEmpty()) {
            return false;
        }

        if (!Utils.isReachable(_eventsEndpoint)) {
            Logger.i("%s is NOT REACHABLE. Sending impressions will be delayed until host is reachable", _eventsEndpoint.getHost());
            return false;
        }

        synchronized (this) {
            Logger.d("Posting %d Split impressions", impressions.size());
                //entity.setContentType("application/json");
            return post(Json.toJson(impressions));
        }
    }

    private boolean post(String data) {

        try {
            HttpResponse response = _client.request(_eventsEndpoint, HttpClient.HTTP_POST, data).execute();

            //TODO: Reason
            String reason = "";
            if (!response.isSuccess()) {
                Logger.w("Response status was: %d. Reason: %s", response.getHttpStatus(), reason);
                return false;
            }
            Logger.d("Entity sent: %s", data);

            return true;
        } catch (Throwable t) {
            Logger.e(t, "Exception when posting impressions %s", data);
            return false;
        }

    }

}
