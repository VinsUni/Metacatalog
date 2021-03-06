package fr.cnes.sitools.metacatalogue.security;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.restlet.Client;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.engine.Engine;
import org.restlet.security.Verifier;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * The Class OAuthVerifier.
 * 
 * @author m.gond,tx.chevallier
 */
public class OAuthVerifier implements Verifier {

  /** The url of the Oauth validation service */
  private String url;

  /** A cache to store token/userlogin mapping not to search for it if it has already been searched for */
  private Cache<String, Integer> tokenCache;

  /** whether or not to use a cache */
  private boolean cacheToken;

  /** The Logger */
  private Logger logger;

  /**
   * Instantiates a new Oauth verifier.
   * 
   * @param url
   *          the url of the Oauth validation service
   */
  public OAuthVerifier(String url) {
    this(url, false, 0, 0);
  }

  /**
   * Instantiates a new Oauth verifier.
   * 
   * @param url
   *          the url of the Oauth validation service
   * @param cacheToken
   *          true to use a cache, false otherwise
   * @param cacheSize
   *          the cacheSize
   * @param expireTime
   *          the cache expire time in Minutes
   */
  public OAuthVerifier(String url, boolean cacheToken, int cacheSize, int expireTime) {
    super();
    // Register a new OauthHelper
    ChallengeScheme scheme = SitoolsChallengeScheme.HTTP_BEARER;
    OAuthHelper helper = new OAuthHelper(scheme, true, true);
    Engine.getInstance().getRegisteredAuthenticators().add(helper);

    // create a new tokenCache
    this.cacheToken = cacheToken;
    if (cacheToken) {
      tokenCache = CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterWrite(expireTime, TimeUnit.MINUTES)
          .build();
    }

    this.url = url;
    logger = Engine.getLogger(OAuthVerifier.class.getName());

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.restlet.security.Verifier#verify(org.restlet.Request, org.restlet.Response)
   */
  @Override
  public int verify(Request request, Response response) {

    ChallengeResponse challengeResponse = request.getChallengeResponse();
    if (challengeResponse == null || !challengeResponse.getScheme().equals(SitoolsChallengeScheme.HTTP_BEARER)) {
      return Verifier.RESULT_MISSING;
    }
    int result;
    try {
      String token = challengeResponse.getRawValue();
      if (this.cacheToken && this.tokenCache.getIfPresent(token) != null) {
        logger.fine("GET TOKEN FROM CACHE");
        return this.tokenCache.getIfPresent(token);
      }
      Reference ref = new Reference(url);

      Client client = new Client(ref.getSchemeProtocol());
      Request req = new Request(Method.GET, url);

      req.setChallengeResponse(challengeResponse);
      Response resp = client.handle(req);
      if (resp.getStatus().isSuccess()) {
        // read the suggest JSON 
        ObjectMapper mapper = new ObjectMapper();
        // (note: can also use more specific type, like ArrayNode or ObjectNode!)
        JsonNode rootNode = mapper.readValue(resp.getEntity().getStream(), JsonNode.class); // src can be a File, URL,
        
        if (rootNode.has("success") && rootNode.get("success") != null) {
          result = Verifier.RESULT_VALID;
        }
        else {
          result = Verifier.RESULT_MISSING;
        }
        if (this.cacheToken) {
          tokenCache.put(token, result);
        }
      }
      else {
        logger.log(Level.WARNING, "Error while calling SSO token validation", resp.getStatus().getThrowable());
        result = Verifier.RESULT_MISSING;
      }
    }
    catch (Exception e) {
      logger.log(Level.WARNING, "Error while calling SSO token validation", e);
      result = Verifier.RESULT_MISSING;
    }
    return result;
  }

}
