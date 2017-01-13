/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.connector.maas.oauth;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * @author ActiveEon Team
 * @since 10/01/17
 */
class SigningSupport {

    private TimestampGenerator timestampGenerator = new DefaultTimestampGenerator();

    /**
     * Builds the authorization header.
     * The elements in additionalParameters are expected to not be encoded.
     */
    public String buildAuthorizationHeaderValue(HttpMethod method, URI targetUrl, Map<String, String> oauthParameters, MultiValueMap<String, String> additionalParameters, String consumerSecret, String tokenSecret) {
        StringBuilder header = new StringBuilder();
        header.append("OAuth ");
        for (Entry<String, String> entry : oauthParameters.entrySet()) {
            header.append(oauthEncode(entry.getKey())).append("=\"").append(oauthEncode(entry.getValue())).append("\", ");
        }
        MultiValueMap<String, String> collectedParameters = new LinkedMultiValueMap<String, String>((int) ((oauthParameters.size() + additionalParameters.size()) / .75 + 1));
        collectedParameters.setAll(oauthParameters);
        collectedParameters.putAll(additionalParameters);
        //String baseString = buildBaseString(method, getBaseStringUri(targetUrl), collectedParameters);
        StringBuilder signature = new StringBuilder().append(consumerSecret).append('&').append(tokenSecret);
        header.append(oauthEncode("oauth_signature")).append("=\"").append(oauthEncode(signature.toString())).append("\"");
        return header.toString();
    }

    /**
     * Builds an authorization header from a request.
     * Expects that the request's query parameters are form-encoded.
     */
    public String buildAuthorizationHeaderValue(HttpRequest request, byte[] body, String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        Map<String, String> oauthParameters = commonOAuthParameters(consumerKey);
        oauthParameters.put("oauth_token", accessToken);
        MultiValueMap<String, String> additionalParameters = union(readFormParameters(request.getHeaders().getContentType(), body), parseFormParameters(request.getURI().getRawQuery()));
        return buildAuthorizationHeaderValue(request.getMethod(), request.getURI(), oauthParameters, additionalParameters, consumerSecret, accessTokenSecret);
    }

    /**
     * Builds an authorization header from a request.
     * Expects that the request's query parameters are form-encoded.
     * This method is a Spring 3.0-compatible version of buildAuthorizationHeaderValue(); planned for removal in Spring Social 1.1
     */
    public String spring30buildAuthorizationHeaderValue(ClientHttpRequest request, byte[] body, String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        Map<String, String> oauthParameters = commonOAuthParameters(consumerKey);
        oauthParameters.put("oauth_token", accessToken);
        MultiValueMap<String, String> additionalParameters = union(readFormParameters(request.getHeaders().getContentType(), body), parseFormParameters(request.getURI().getRawQuery()));
        return buildAuthorizationHeaderValue(request.getMethod(), request.getURI(), oauthParameters, additionalParameters, consumerSecret, accessTokenSecret);
    }

    Map<String, String> commonOAuthParameters(String consumerKey) {
        Map<String, String> oauthParameters = new HashMap<String, String>();
        oauthParameters.put("oauth_consumer_key", consumerKey);
        oauthParameters.put("oauth_signature_method", PLAINTEXT_SIGNATURE_NAME);
        long timestamp = timestampGenerator.generateTimestamp();
        oauthParameters.put("oauth_timestamp", Long.toString(timestamp));
        oauthParameters.put("oauth_nonce", Long.toString(timestampGenerator.generateNonce(timestamp)));
        oauthParameters.put("oauth_version", "1.0");
        return oauthParameters;
    }

    String buildBaseString(HttpMethod method, String targetUrl, MultiValueMap<String, String> collectedParameters) {
        StringBuilder builder = new StringBuilder();
        builder.append(method.name()).append('&').append(oauthEncode(targetUrl)).append('&');
        builder.append(oauthEncode(normalizeParameters(collectedParameters)));
        return builder.toString();
    }

    // testing hooks

    // tests can implement and inject a custom TimestampGenerator to work with fixed nonce and timestamp values

    void setTimestampGenerator(SigningSupport.TimestampGenerator timestampGenerator) {
        this.timestampGenerator = timestampGenerator;
    }

    static interface TimestampGenerator {

        long generateTimestamp();

        long generateNonce(long timestamp);

    }

    private static class DefaultTimestampGenerator implements TimestampGenerator {

        public long generateTimestamp() {
            return System.currentTimeMillis() / 1000;
        }

        public long generateNonce(long timestamp) {
            return timestamp + RANDOM.nextInt();
        }

        static final Random RANDOM = new Random();

    }

    // internal helpers

    private String normalizeParameters(MultiValueMap<String, String> collectedParameters) {
        // Normalizes the collected parameters for baseString calculation, per http://tools.ietf.org/html/rfc5849#section-3.4.1.3.2
        MultiValueMap<String, String> sortedEncodedParameters = new TreeMultiValueMap<String, String>();
        for (Iterator<Entry<String, List<String>>> entryIt = collectedParameters.entrySet().iterator(); entryIt.hasNext();) {
            Entry<String, List<String>> entry = entryIt.next();
            String collectedName = entry.getKey();
            List<String> collectedValues = entry.getValue();
            List<String> encodedValues = new ArrayList<String>(collectedValues.size());
            sortedEncodedParameters.put(oauthEncode(collectedName), encodedValues);
            for (Iterator<String> valueIt = collectedValues.iterator(); valueIt.hasNext();) {
                String value = valueIt.next();
                encodedValues.add(value != null ? oauthEncode(value) : "");
            }
            Collections.sort(encodedValues);
        }
        StringBuilder paramsBuilder = new StringBuilder();
        for (Iterator<Entry<String, List<String>>> entryIt = sortedEncodedParameters.entrySet().iterator(); entryIt.hasNext();) {
            Entry<String, List<String>> entry = entryIt.next();
            String name = entry.getKey();
            List<String> values = entry.getValue();
            for (Iterator<String> valueIt = values.iterator(); valueIt.hasNext();) {
                String value = valueIt.next();
                paramsBuilder.append(name).append('=').append(value);
                if (valueIt.hasNext()) {
                    paramsBuilder.append("&");
                }
            }
            if (entryIt.hasNext()) {
                paramsBuilder.append("&");
            }
        }
        return paramsBuilder.toString();
    }

    private MultiValueMap<String, String> readFormParameters(MediaType bodyType, byte[] bodyBytes) {
        if (bodyType != null && bodyType.equals(MediaType.APPLICATION_FORM_URLENCODED)) {
            String body = new String(bodyBytes, charset);
            return parseFormParameters(body);
        } else {
            return EmptyMultiValueMap.instance();
        }
    }

    private MultiValueMap<String, String> parseFormParameters(String parameterString) {
        if (parameterString == null || parameterString.length() == 0) {
            return EmptyMultiValueMap.instance();
        }
        String[] pairs = StringUtils.tokenizeToStringArray(parameterString, "&");
        MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>(pairs.length);
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx == -1) {
                result.add(formDecode(pair), "");
            }
            else {
                String name = formDecode(pair.substring(0, idx));
                String value = formDecode(pair.substring(idx + 1));
                result.add(name, value);
            }
        }
        return result;
    }

    private String getBaseStringUri(URI uri) {
        try {
            // see: http://tools.ietf.org/html/rfc5849#section-3.4.1.2
            return new URI(uri.getScheme(), null, uri.getHost(), getPort(uri), uri.getPath(), null, null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // can't use putAll here because it will overwrite anything that has the same key in both maps
    private MultiValueMap<String, String> union(MultiValueMap<String, String> map1, MultiValueMap<String, String> map2) {
        MultiValueMap<String, String> union = new LinkedMultiValueMap<String, String>(map1);
        Set<Entry<String, List<String>>> map2Entries = map2.entrySet();
        for (Iterator<Entry<String, List<String>>> entryIt = map2Entries.iterator(); entryIt.hasNext();) {
            Entry<String, List<String>> entry = entryIt.next();
            String key = entry.getKey();
            List<String> values = entry.getValue();
            for (String value : values) {
                union.add(key, value);
            }
        }
        return union;
    }

    private int getPort(URI uri) {
        if (uri.getScheme().equals("http") && uri.getPort() == 80 || uri.getScheme().equals("https") && uri.getPort() == 443) {
            return -1;
        } else {
            return uri.getPort();
        }
    }

    private static final BitSet UNRESERVED;

    static {
        BitSet alpha = new BitSet(256);
        for (int i = 'a'; i <= 'z'; i++) {
            alpha.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            alpha.set(i);
        }
        BitSet digit = new BitSet(256);
        for (int i = '0'; i <= '9'; i++) {
            digit.set(i);
        }
        BitSet unreserved = new BitSet(256);
        unreserved.or(alpha);
        unreserved.or(digit);
        unreserved.set('-');
        unreserved.set('.');
        unreserved.set('_');
        unreserved.set('~');
        UNRESERVED = unreserved;
    }

    private static String oauthEncode(String param) {
        try {
            // See http://tools.ietf.org/html/rfc5849#section-3.6
            byte[] bytes = encode(param.getBytes("UTF-8"), UNRESERVED);
            return new String(bytes, "US-ASCII");
        } catch (Exception shouldntHappen) {
            throw new IllegalStateException(shouldntHappen);
        }
    }

    private static byte[] encode(byte[] source, BitSet notEncoded) {
        Assert.notNull(source, "'source' must not be null");
        ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length * 2);
        for (int i = 0; i < source.length; i++) {
            int b = source[i];
            if (b < 0) {
                b += 256;
            }
            if (notEncoded.get(b)) {
                bos.write(b);
            }
            else {
                bos.write('%');
                char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
                bos.write(hex1);
                bos.write(hex2);
            }
        }
        return bos.toByteArray();
    }

    private static String formDecode(String encoded) {
        try {
            return URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException shouldntHappen) {
            throw new IllegalStateException(shouldntHappen);
        }
    }

    private static final String HMAC_SHA1_SIGNATURE_NAME = "HMAC-SHA1";
    private static final String PLAINTEXT_SIGNATURE_NAME = "PLAINTEXT";

    private static final String HMAC_SHA1_MAC_NAME = "HmacSHA1";

    private static Charset charset = Charset.forName("UTF-8");

}
