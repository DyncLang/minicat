package com.fanfou.app.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import com.fanfou.app.App;
import com.fanfou.app.api.ApiConfig;
import com.fanfou.app.api.ApiException;
import com.fanfou.app.http.ConnectionManager;
import com.fanfou.app.http.ConnectionRequest;
import com.fanfou.app.http.Parameter;
import com.fanfou.app.util.Base64;

/**
 * @author mcxiaoke
 * @version 2.0 2011.11.03
 *
 */
public class OAuth {

	private static final String TAG = OAuth.class.getSimpleName();

	void log(String message) {
		Log.d(TAG, message);
	}

	public static final String REQUEST_TOKEN_URL = ApiConfig.HOST
			+ "oauth/request_token";
	public static final String AUTHENTICATE_URL = ApiConfig.HOST
			+ "oauth/authenticate";
	public static final String AUTHORIZE_URL = ApiConfig.HOST
			+ "oauth/authorize";
	public static final String ACCESS_TOKEN_URL = ApiConfig.HOST
			+ "oauth/access_token";

	public static final String X_AUTH_USERNAME = "x_auth_username";
	public static final String X_AUTH_PASSWORD = "x_auth_password";
	public static final String X_AUTH_MODE = "x_auth_mode";// client_auth

	private static final String HMAC_SHA1 = "HmacSHA1";
	private static final Parameter OAUTH_SIGNATURE_METHOD = new Parameter(
			"oauth_signature_method", "HMAC-SHA1");
	private static final long serialVersionUID = -4368426677157998618L;

	private OAuthToken oauthToken = null;

	public OAuth() {

	}

	public OAuth(String token, String tokenSecret) {
		this.oauthToken = new OAuthToken(token, tokenSecret);
	}

	public void signRequest(HttpUriRequest request, List<Parameter> params) {
		String authorization = getOAuthHeader(request.getMethod(), request
				.getURI().toString(), params, oauthToken);
		request.setHeader(new BasicHeader("Authorization", authorization));
	}

	public OAuthToken getOAuthAccessToken(String username, String password)
			throws ApiException, IOException {
		String authorization = getXAuthHeader(username, password,HttpGet.METHOD_NAME, ACCESS_TOKEN_URL);
//		if (App.DEBUG)
//			log("xauth authorization=" + authorization);
//		HttpGet request = new HttpGet(ACCESS_TOKEN_URL);
//		request.addHeader(new BasicHeader("Authorization", authorization));
//		HttpClient client = NetworkHelper.newHttpClient();
//		NetworkHelper.setProxy(client);
//		HttpResponse response = client.execute(request);
		ConnectionRequest.Builder builder=new ConnectionRequest.Builder();
		builder.url(ACCESS_TOKEN_URL);
		builder.header("Authorization", authorization);
		HttpResponse response = ConnectionManager.execNoAuth(builder.build());
		int statusCode = response.getStatusLine().getStatusCode();
		String content = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
		if (App.DEBUG)
			log("getOAuthAccessToken() code=" + statusCode + " response="
					+ content);
		if (statusCode == 200) {
			return new OAuthToken(content);
		} else {
			if (App.DEBUG){
				log("getOAuthAccessToken content=" + content);}
			throw new ApiException(statusCode, "帐号或密码不正确，登录失败");
		}
	}

	public void setAccessToken(OAuthToken accessToken) {
		this.oauthToken = accessToken;
	}

	private String getXAuthHeader(String username, String password,
			String method, String url) {
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime() + RAND.nextInt();
		List<Parameter> oauthHeaderParams = new ArrayList<Parameter>();
		oauthHeaderParams.add(new Parameter("oauth_consumer_key",
				ApiConfig.CONSUMER_KEY));
		oauthHeaderParams.add(OAUTH_SIGNATURE_METHOD);
		oauthHeaderParams.add(new Parameter("oauth_timestamp", timestamp));
		oauthHeaderParams.add(new Parameter("oauth_nonce", nonce));
		oauthHeaderParams.add(new Parameter("oauth_version", "1.0"));
		oauthHeaderParams.add(new Parameter("x_auth_username", username));
		oauthHeaderParams.add(new Parameter("x_auth_password", password));
		oauthHeaderParams.add(new Parameter("x_auth_mode", "client_auth"));
		// parseGetParams(url, oauthHeaderParams);
		StringBuffer base = new StringBuffer(method).append("&")
				.append(ConnectionRequest.encode(constructRequestURL(url))).append("&");
		base.append(ConnectionRequest.encode(alignParams(oauthHeaderParams)));
		String oauthBaseString = base.toString();
		String signature = signature(oauthBaseString, null);
		oauthHeaderParams.add(new Parameter("oauth_signature", signature));
		return "OAuth " + encodeParameters(oauthHeaderParams, ",", true);
	}

	private void parseGetParams(String url, List<Parameter> signatureBaseParams) {
		int queryStart = url.indexOf("?");
		if (-1 != queryStart) {
			String[] queryStrs = url.substring(queryStart + 1).split("&");
			try {
				for (String query : queryStrs) {
					String[] split = query.split("=");
					if (split.length == 2) {
						signatureBaseParams.add(new Parameter(URLDecoder
								.decode(split[0], "UTF-8"), URLDecoder.decode(
								split[1], "UTF-8")));
					} else {
						signatureBaseParams.add(new Parameter(URLDecoder
								.decode(split[0], "UTF-8"), ""));
					}
				}
			} catch (UnsupportedEncodingException ignore) {
			}

		}

	}

	private static Random RAND = new Random();

	public String getOAuthHeader(String method, String url,
			List<Parameter> params, OAuthToken otoken) {
		if (params == null) {
			params = new ArrayList<Parameter>();
		}
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = timestamp + RAND.nextInt();
		List<Parameter> oauthHeaderParams = new ArrayList<Parameter>();
		oauthHeaderParams.add(new Parameter("oauth_consumer_key",
				ApiConfig.CONSUMER_KEY));
		oauthHeaderParams.add(OAUTH_SIGNATURE_METHOD);
		oauthHeaderParams.add(new Parameter("oauth_timestamp", timestamp));
		oauthHeaderParams.add(new Parameter("oauth_nonce", nonce));
		oauthHeaderParams.add(new Parameter("oauth_version", "1.0"));
		if (null != otoken) {
			if (App.DEBUG)
				log("getOAuthHeader() add oauth token to header: oauth_token="
						+ otoken.getToken()+" oauth_token_secret="+otoken.getTokenSecret());
			oauthHeaderParams.add(new Parameter("oauth_token", otoken
					.getToken()));
		}
		List<Parameter> signatureBaseParams = new ArrayList<Parameter>(
				oauthHeaderParams.size() + params.size());
		signatureBaseParams.addAll(oauthHeaderParams);
		if (method != HttpGet.METHOD_NAME && params != null
				&& !ConnectionRequest.containsFile(params)) {
			signatureBaseParams.addAll(params);
		}
		parseGetParams(url, signatureBaseParams);
		if(App.DEBUG){
			log("getOAuthHeader() url="+url);
		}
		StringBuffer base = new StringBuffer(method).append("&")
				.append(ConnectionRequest.encode(constructRequestURL(url))).append("&");
		base.append(ConnectionRequest.encode(alignParams(signatureBaseParams)));
		String oauthBaseString = base.toString();
		String signature = signature(oauthBaseString, otoken);
		oauthHeaderParams.add(new Parameter("oauth_signature", signature));
		return "OAuth " + encodeParameters(oauthHeaderParams, ",", true);
	}

	List<Parameter> signatureParams(String method, String url) {
		long timestamp = System.currentTimeMillis() / 1000;
		long nonce = System.nanoTime() + RAND.nextInt();

		List<Parameter> oauthHeaderParams = new ArrayList<Parameter>(5);
		oauthHeaderParams.add(new Parameter("oauth_consumer_key",
				ApiConfig.CONSUMER_KEY));
		oauthHeaderParams.add(OAUTH_SIGNATURE_METHOD);
		oauthHeaderParams.add(new Parameter("oauth_timestamp", timestamp));
		oauthHeaderParams.add(new Parameter("oauth_nonce", nonce));
		oauthHeaderParams.add(new Parameter("oauth_version", "1.0"));
		if (null != oauthToken) {
			oauthHeaderParams.add(new Parameter("oauth_token", oauthToken
					.getToken()));
		}

		List<Parameter> signatureBaseParams = new ArrayList<Parameter>(
				oauthHeaderParams.size());
		signatureBaseParams.addAll(oauthHeaderParams);

		// log("=========createOAuthSignatureParams========");
		if (App.DEBUG) {
			for (Parameter parameter : signatureBaseParams) {
				log(parameter.toString());
			}
		}

		parseGetParams(url, signatureBaseParams);

		StringBuffer base = new StringBuffer(method).append("&")
				.append(ConnectionRequest.encode(constructRequestURL(url))).append("&");
		base.append(ConnectionRequest.encode(alignParams(signatureBaseParams)));

		String oauthBaseString = base.toString();
		String signature = signature(oauthBaseString, oauthToken);

		oauthHeaderParams.add(new Parameter("oauth_signature", signature));

		return oauthHeaderParams;
	}

	String signature(String data, OAuthToken token) {
		// log("createSignature baseString=" + data + " token="
		// + (token == null ? "null" : token.getToken()));
		byte[] byteHMAC = null;
		try {
			Mac mac = Mac.getInstance(HMAC_SHA1);
			SecretKeySpec spec;
			if (null == token) {
				String oauthSignature = ConnectionRequest
						.encode(ApiConfig.CONSUMER_SECRET) + "&";
				spec = new SecretKeySpec(oauthSignature.getBytes(), HMAC_SHA1);
			} else {
				spec = token.getSecretKeySpec();
				if (null == spec) {
					String oauthSignature = ConnectionRequest
							.encode(ApiConfig.CONSUMER_SECRET)
							+ "&"
							+ ConnectionRequest.encode(token.getTokenSecret());
					spec = new SecretKeySpec(oauthSignature.getBytes(),
							HMAC_SHA1);
					token.setSecretKeySpec(spec);
				}
			}
			mac.init(spec);
			byteHMAC = mac.doFinal(data.getBytes());
		} catch (InvalidKeyException ike) {
			throw new AssertionError(ike);
		} catch (NoSuchAlgorithmException nsae) {
			throw new AssertionError(nsae);
		}
		return Base64.encodeBytes(byteHMAC);
	}

	public static String alignParams(List<Parameter> params) {
		Collections.sort(params);
		return encodeParameters(params);
	}

	public static String encodeParameters(List<Parameter> httpParams) {
		return encodeParameters(httpParams, "&", false);
	}

	public static String encodeParameters(List<Parameter> httpParams,
			String splitter, boolean quot) {
		StringBuffer buf = new StringBuffer();
		for (Parameter param : httpParams) {
			if (!param.isFile()) {
				if (buf.length() != 0) {
					if (quot) {
						buf.append("\"");
					}
					buf.append(splitter);
				}
				buf.append(ConnectionRequest.encode(param.getName())).append("=");
				if (quot) {
					buf.append("\"");
				}
				buf.append(ConnectionRequest.encode(param.getValue()));
			}
		}
		if (buf.length() != 0) {
			if (quot) {
				buf.append("\"");
			}
		}
		return buf.toString();
	}

	public static String constructRequestURL(String url) {
		int index = url.indexOf("?");
		if (-1 != index) {
			url = url.substring(0, index);
		}
		int slashIndex = url.indexOf("/", 8);
		String baseURL = url.substring(0, slashIndex).toLowerCase();
		int colonIndex = baseURL.indexOf(":", 8);
		if (-1 != colonIndex) {
			if (baseURL.startsWith("http://") && baseURL.endsWith(":80")) {
				baseURL = baseURL.substring(0, colonIndex);
			} else if (baseURL.startsWith("https://")
					&& baseURL.endsWith(":443")) {
				baseURL = baseURL.substring(0, colonIndex);
			}
		}
		url = baseURL + url.substring(slashIndex);

		return url;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		return ApiConfig.CONSUMER_KEY.hashCode();
	}

	@Override
	public String toString() {
		return "OAuth { " + ", oauthToken=" + oauthToken + '}';
	}
}
