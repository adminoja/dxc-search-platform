package th.go.dxc.platform.search.application.common.security;

public interface ScopeHasher {
  String scopeFor(String userId, String tenant, String realm);
  // default String scopeFor(String userId, String tenant, String realm, @Nullable String sessionId) {
  //   return scopeFor(userId, tenant, realm) + (sessionId != null ? (":" + sessionId) : "");
  // }
}
