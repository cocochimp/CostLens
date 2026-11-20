package com.costlens.infrastructure.cost.mysql.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_cost_sheet")
public class OrderCostSheetPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sheetId;
    private String orderNo;
    private String platform;
    private Long revenueMinor;
    private Long totalCostMinor;
    private String currency;
    private String status;
    private String recalcBatchId;
    private LocalDateTime createdAt;
}