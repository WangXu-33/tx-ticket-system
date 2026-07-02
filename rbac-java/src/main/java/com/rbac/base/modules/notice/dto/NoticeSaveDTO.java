package com.rbac.base.modules.notice.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NoticeSaveDTO {
    private Long id;
    private String title;
    private String content;
    private String noticeType;
    private String priority;
    private String remark;
    private List<NoticeTargetDTO> targets = new ArrayList<>();
}
