package org.obiba.oidc.shiro.realm;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.json.JSONArray;
import org.obiba.oidc.OIDCConfiguration;
import org.obiba.oidc.OIDCCredentials;
import org.obiba.oidc.shiro.authc.OIDCAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Realm based on OpenID connect token, after all authorization and identification stuff has happen.
 */
public class OIDCRealm extends AuthorizingRealm {

  private static final Logger log = LoggerFactory.getLogger(OIDCRealm.class);

  private final OIDCConfiguration configuration;

  public OIDCRealm(OIDCConfiguration configuration) {
    setName(configuration.getName());
    this.configuration = configuration;
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    return token instanceof OIDCAuthenticationToken;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    OIDCCredentials credentials = (OIDCCredentials) token.getCredentials();
    try {
      JWTClaimsSet claims = credentials.getIdToken().getJWTClaimsSet();
      String issuer = claims.getIssuer();
      if (!configuration.findProviderMetaData().getIssuer().toString().equals(issuer)) return null;
    } catch (ParseException e) {
      log.debug("Error while accessing the claims for OIDC realm {}", getName());
      return null;
    }
    Map<String, Object> userInfo = credentials.getUserInfo();
    List<Object> principals = Lists.newArrayList(((OIDCAuthenticationToken) token).getUsername());
    if (userInfo != null) {
      principals.add(userInfo);
      log.info("OIDC realm {}, received userInfo {}", getName(), userInfo);
    }
    final PrincipalCollection principalCollection = new SimplePrincipalCollection(principals, getName());
    return new SimpleAuthenticationInfo(principalCollection, token.getCredentials());
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    Collection<?> thisPrincipals = principals.fromRealm(getName());

    if(thisPrincipals != null && !thisPrincipals.isEmpty()) {
      String groupsParam = configuration.getCustomParam("groups");
      Set<String> groups = Sets.newHashSet(getName());
      if (!Strings.isNullOrEmpty(groupsParam)) {
        extractGroups(groupsParam).forEach(groups::add);
      }
      for (Object principal : thisPrincipals) {
        if (principal instanceof Map) {
          try {
            // TODO improve by getting groups key from OIDC config
            Object gps = ((Map<String, Object>) principal).get("groups");
            if (gps != null) {
              extractGroups(gps.toString()).forEach(groups::add);
            }
          } catch (Exception e) {
            log.debug("Principal: {}", principal);
            log.warn("Failed at retrieving userInfo from principal", e);
          }
        }
      }
      return new SimpleAuthorizationInfo(groups);
    }
    return new SimpleAuthorizationInfo();
  }

  private Iterable<String> extractGroups(String groupsParam) {
    if (groupsParam.startsWith("[") && groupsParam.endsWith("]")) {
      // expect a json array
      return new JSONArray(groupsParam).toList().stream()
          .filter(Objects::nonNull)
          .map(Object::toString)
          .collect(Collectors.toList());
    }
    return Splitter.on(" ").omitEmptyStrings().trimResults().split(groupsParam);
  }

}
