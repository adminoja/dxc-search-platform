package th.go.dxc.platform.search.domain.common.value;

// Initiator.java
public enum Initiator {
  USER,      // user-triggered via UI/API
  SYSTEM     // scheduled job, webhook, retry worker, etc.
}