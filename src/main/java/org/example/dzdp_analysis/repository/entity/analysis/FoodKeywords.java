package org.example.dzdp_analysis.repository.entity.analysis;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "food_keywords")
public class FoodKeywords {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "keyword")
    private String keyword;

    @Column(name = "mention_count")
    private Integer mentionCount;

    @Column(name = "update_date")
    private String updateDate;

    @Column(name = "create_time")
    private Date createTime;

    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Integer getMentionCount() { return mentionCount; }
    public void setMentionCount(Integer mentionCount) { this.mentionCount = mentionCount; }

    public String getUpdateDate() { return updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}