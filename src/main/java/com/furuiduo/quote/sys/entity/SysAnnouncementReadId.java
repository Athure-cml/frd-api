package com.furuiduo.quote.sys.entity;

import java.io.Serializable;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SysAnnouncementReadId implements Serializable {

  private Long announcementId;
  private Long userId;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SysAnnouncementReadId that)) {
      return false;
    }
    return Objects.equals(announcementId, that.announcementId)
        && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(announcementId, userId);
  }
}
