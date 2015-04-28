package io.github.kirillf.mapview.http;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Synchronous http request/response processor.
 * Should not be used in UI thread.
 */
public class HttpService implements Service<HttpRequest, HttpResponse> {
    private static final String TAG = HttpService.class.getName();

    @Override
    public HttpResponse doRequest(HttpRequest httpRequest) {
        try {
            URL url = new URL(httpRequest.getUrl());
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod(httpRequest.getMethod().getRequestParam());
            httpURLConnection.setConnectTimeout(httpRequest.getConnectionTimeout());
            httpURLConnection.setReadTimeout(httpRequest.getReadTimeout());
            httpURLConnection.setDoInput(true);
            for (Map.Entry<String, String> param : httpRequest.getHeaders().entrySet()) {
                httpURLConnection.setRequestProperty(param.getKey(), param.getValue());
            }
            if (httpRequest.getMethod().equals(HttpRequest.Method.POST)) {
                httpURLConnection.setDoOutput(true);
                String body = httpRequest.getBody();
                if (body != null) {
                    byte[] bytes = body.getBytes();
                    httpURLConnection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
                    OutputStream outputStream = httpURLConnection.getOutputStream();
                    outputStream.write(bytes);
                    outputStream.close();
                }
            }
            httpURLConnection.connect();
            int reponseCode = httpURLConnection.getResponseCode();
            HttpResponse httpResponse = new HttpResponse(reponseCode);
            try {
                httpResponse.setContent(getResponseContent(httpURLConnection));
            } catch (IOException e) {
                Log.w(TAG, getErrorMessage(httpURLConnection));
                Log.w(TAG, e);
            }
            httpURLConnection.disconnect();
            return httpResponse;
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return null;
    }

    private String getErrorMessage(HttpURLConnection httpURLConnection) throws IOException {
        InputStream inputStream = httpURLConnection.getErrorStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private byte[] getResponseContent(HttpURLConnection httpURLConnection) throws IOException {
        InputStream inputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            inputStream = httpURLConnection.getInputStream();
            int data;
            while ((data = inputStream.read()) != -1) {
                byteArrayOutputStream.write(data);
            }
            return byteArrayOutputStream.toByteArray();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            byteArrayOutputStream.close();
        }
    }

}
