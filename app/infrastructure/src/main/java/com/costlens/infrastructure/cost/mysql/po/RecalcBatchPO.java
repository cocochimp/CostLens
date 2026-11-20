package com.costlens.infrastructure.cost.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recalc_batch")
public class RecalcBatchPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchId;
    private String mode;
    private LocalDateTime periodFrom;
    private LocalDateTime periodTo;
    private Integer shardTotal;
    private Integer shardFinished;
    private Integer shardFailed;
    private String status;
    private String diffSummary;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}