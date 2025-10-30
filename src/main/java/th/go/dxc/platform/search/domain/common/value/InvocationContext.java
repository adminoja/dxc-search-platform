package th.go.dxc.platform.search.domain.common.value;

// InvocationContext.java
public record InvocationContext(
    String traceId, // correlation across services
    String userActionId, // one per user "click/submit"
    Initiator initiator, // USER or SYSTEM
    FeatureType productFeature, // the user-facing feature you count for analytics
    String productFeatureId, // reportId or runId (nullable for top-level Local Search)
    FeatureType currentFeature, // the feature executing right now
    String currentFeatureId // runId/reportId/etc. (nullable for composed Local Search)
) {
  public InvocationContext withCurrent(FeatureType feature, String id) {
    return new InvocationContext(
        traceId, userActionId, initiator, productFeature, productFeatureId, feature, id);
  }

  public boolean isStandaloneLocal() {
    return productFeature == FeatureType.LOCAL_SEARCH && currentFeature == FeatureType.LOCAL_SEARCH;
  }

  public static InvocationContext userTopLevel(FeatureType feature, String featureId,
      String traceId, String userActionId) {
    return new InvocationContext(traceId, userActionId, Initiator.USER, feature, featureId, feature, featureId);
  }

  public static InvocationContext systemTopLevel(FeatureType feature, String featureId,
      String traceId, String actionId) {
    return new InvocationContext(traceId, actionId, Initiator.SYSTEM, feature, featureId, feature, featureId);
  }

  public enum FeatureType {
    LOCAL_SEARCH,
    GLOBAL_SEARCH,
    SINGLE_REPORT,
    SPECIALIZED_REPORT
  }

  public enum Initiator {
    USER,
    SYSTEM
  }
  
}
