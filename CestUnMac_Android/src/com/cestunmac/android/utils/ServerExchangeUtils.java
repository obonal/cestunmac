package com.cestunmac.android.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.cestunmac.android.Constants;

public class ServerExchangeUtils {

    /**
     * 
     * @param json_object
     * @param json_handler_callback
     * @param json_param_name
     * @param gzipped
     */
    public static synchronized void postJSon(Context _context,
                                             JSONArray json_object,
                                             IJSonResponseHandler json_handler_callback,
                                             String json_param_name,
                                             boolean gzipped) {
        postJSon(_context, json_object.toString(), json_handler_callback, json_param_name, gzipped);
    }

    public static synchronized void postJSon(Context _context,
                                             JSONObject json_object,
                                             IJSonResponseHandler json_handler_callback,
                                             String json_param_name,
                                             boolean gzipped) {
        postJSon(_context, json_object.toString(), json_handler_callback, json_param_name, gzipped);
    }

    public static synchronized JsonNode postJSonForResponse(Context _context, String json_string, String json_param_name, boolean gzipped) throws Throwable {
        JsonNode result_root_node = null;

        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getConnectionManager().closeExpiredConnections();
        httpclient.getParams().setParameter("http.socket.timeout", new Integer(20000)); // 20 second
        // Prepare a request object
        HttpPost httppost = new HttpPost(Constants.JSON_POST_URL);

        // Accept: gzip
        if (gzipped) {
            httppost.addHeader("Accept-Encoding", "gzip");
        }

        httppost.setHeader("Content-type", "application/json");

        Log.i(Constants.LOG_TAG, "---------------------------------------------");
        Log.i(Constants.LOG_TAG, "Posting json: " + json_string);
        Log.i(Constants.LOG_TAG, "With Headers: ");
        HeaderIterator it = httppost.headerIterator();
        while (it.hasNext()) {
            Header h = (Header) it.next();
            Log.i(Constants.LOG_TAG, h.getName() + ":" + h.getValue());

        }
        Log.i(Constants.LOG_TAG, "---------------------------------------------");

        // Execute the request
        HttpResponse response;
        HttpEntity post_entity;
        InputStream instream = null;

        try {
            post_entity = new StringEntity(json_string, HTTP.UTF_8);
            httppost.setEntity(post_entity);
            httppost.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
            // Log.i(Constants.LOG_TAG,"---------------- AVANT le httpclient.execute(httppost) --------------");
            response = httpclient.execute(httppost);
            // Log.i(Constants.LOG_TAG,"---------------- APRES le httpclient.execute(httppost) --------------");
            // Examine the response status
            Log.i(Constants.LOG_TAG, response.getStatusLine().toString());
            // Get hold of the response entity
            HttpEntity entity = response.getEntity();
            // If the response does not enclose an entity, there is no need
            // to worry about connection release
            if (entity != null) {

                // A Simple JSON Response Read
                instream = entity.getContent();
                
                result_root_node = parseJson(instream, gzipped);

                // Closing the input stream will trigger connection release
                instream.close();

                entity.consumeContent();

                httpclient.getConnectionManager().closeExpiredConnections();
                httpclient.getConnectionManager().shutdown();
                httpclient = null;

                entity = null;
                response = null;
            }
        } finally {
            if (httpclient != null) {
                httpclient.getConnectionManager().shutdown();
            }
        }
        return result_root_node;
    }

    private static synchronized void postJSon(Context _context,
                                              String json_string,
                                              final IJSonResponseHandler json_handler_callback,
                                              String json_param_name,
                                              boolean gzipped) {

        try {
            JsonNode result_root_node = postJSonForResponse(_context, json_string, json_param_name, gzipped);
            if (result_root_node != null) {
                if (json_handler_callback != null) {
                    json_handler_callback.handleJSonResponse(result_root_node);
                }
            } else {
                if (json_handler_callback != null) {
                    json_handler_callback.handleJSonRequestFailure(null);
                }
            }
        } catch (Throwable e) {
            Log.e(Constants.LOG_TAG, "---------------- Exception executing httppost request --------------");
            e.printStackTrace();
            if (json_handler_callback != null) {
                json_handler_callback.handleJSonRequestFailure(e);
            }
        }
    }

    public static JsonNode getRequestForJSonResponse(String request_url_string, Context _context) throws Exception {
        JsonNode result_root_node = null;
        HttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet request = new HttpGet();
            request.setURI(new URI(request_url_string));
            HttpResponse response = httpclient.execute(request);
            // Examine the response status
            Log.i(Constants.LOG_TAG, response.getStatusLine().toString());
            // Get hold of the response entity
            HttpEntity entity = response.getEntity();
            // If the response does not enclose an entity, there is no need
            // to worry about connection release
            if (entity != null) {

                // A Simple JSON Response Read
                InputStream instream = entity.getContent();
                result_root_node = parseJson(instream, false);

                // Closing the input stream will trigger connection release
                instream.close();

                entity.consumeContent();

                httpclient.getConnectionManager().closeExpiredConnections();
                httpclient.getConnectionManager().shutdown();
                httpclient = null;

                entity = null;
                response = null;
            }
        } finally {
            if (httpclient != null) {
                httpclient.getConnectionManager().shutdown();
            }
        }
        return result_root_node;
    }

    private static JsonNode parseJson(InputStream instream, boolean gzipped) {
        try {
            // general method, same as with data binding
            // (note: can also use more specific type, like ArrayNode or ObjectNode!)
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = null;
            if (gzipped) {
                instream = new GZIPInputStream(instream);
            }
            /*
            else {
                String content = CharStreams.toString(new InputStreamReader(instream, Charsets.UTF_16));
                Log.d("DEBUG", "content = " + content);
                rootNode = mapper.readTree(content);
            }
            */
            if (rootNode == null) {
                rootNode = mapper.readValue(instream, JsonNode.class); // src can be a File, URL, InputStream etc
            }
            mapper = null;
            return rootNode;
        } catch (JsonParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getGZippedString(String string_to_gzip) {
        ByteArrayInputStream fin = null;
        GZIPOutputStream gz = null;
        ByteArrayOutputStream fout = null;
        try {
            fin = new ByteArrayInputStream(string_to_gzip.getBytes());
            fout = new ByteArrayOutputStream();
            gz = new GZIPOutputStream(fout);
            byte[] buf = new byte[4096];
            int readCount;
            while ((readCount = fin.read(buf)) != -1) {
                gz.write(buf, 0, readCount);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            // Close the BufferedInputStream
            try {
                if (fin != null) fin.close();
                if (gz != null) gz.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }

        return (gz == null || fout == null) ? null : fout.toByteArray();
    }

    public static Object safeJSonGet(JSONObject _json_object, String _name) {
        try {
            if (_json_object.has(_name)) {
                return _json_object.get(_name);
            } else {
                return null;
            }
        } catch (JSONException e) {
            // TODO: Handle this better
            e.printStackTrace();
            return null;
        }
    }

    public static JsonNode safeJSonGet(JsonNode _json_object, String _name) {
        try {
            if (_json_object.has(_name)) {
                return _json_object.get(_name);
            } else {
                return null;
            }
        } catch (Exception e) {
            // TODO: Handle this better
            e.printStackTrace();
            return null;
        }
    }
}
