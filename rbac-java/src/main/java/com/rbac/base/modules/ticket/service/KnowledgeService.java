package com.rbac.base.modules.ticket.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rbac.base.modules.system.entity.SysFile;
import com.rbac.base.modules.system.mapper.SysFileMapper;
import com.rbac.base.modules.ticket.dto.KnowledgeSaveDTO;
import com.rbac.base.modules.ticket.entity.KnowledgeArticle;
import com.rbac.base.modules.ticket.entity.KnowledgeTicketLink;
import com.rbac.base.modules.ticket.entity.Ticket;
import com.rbac.base.modules.ticket.entity.TicketAttachment;
import com.rbac.base.modules.ticket.mapper.KnowledgeArticleMapper;
import com.rbac.base.modules.ticket.mapper.KnowledgeTicketLinkMapper;
import com.rbac.base.modules.ticket.mapper.TicketAttachmentMapper;
import com.rbac.base.modules.ticket.mapper.TicketMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 代码生成/修改时间：2026-07-01。
 * 知识库业务服务，维护可复用解决方案并关联来源工单。
 * 入参：知识库保存 DTO、分页筛选条件、来源工单 ID。
 * 出参：知识库分页、详情或文章 ID。
 * 异常场景：文章不存在、标题为空、解决步骤为空时抛出 IllegalArgumentException。
 */
@Service
public class KnowledgeService {
    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_REVIEWING = "reviewing";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_REJECTED = "rejected";
    private static final String STATUS_WITHDRAWN = "withdrawn";

    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeTicketLinkMapper linkMapper;
    private final TicketMapper ticketMapper;
    private final TicketAttachmentMapper attachmentMapper;
    private final SysFileMapper sysFileMapper;

    public KnowledgeService(KnowledgeArticleMapper articleMapper,
                            KnowledgeTicketLinkMapper linkMapper,
                            TicketMapper ticketMapper,
                            TicketAttachmentMapper attachmentMapper,
                            SysFileMapper sysFileMapper) {
        this.articleMapper = articleMapper;
        this.linkMapper = linkMapper;
        this.ticketMapper = ticketMapper;
        this.attachmentMapper = attachmentMapper;
        this.sysFileMapper = sysFileMapper;
    }

    public Page<KnowledgeArticle> page(Integer pageNum, Integer pageSize, String keyword, String title, String tags,
                                       String category, String status) {
        Page<KnowledgeArticle> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();

        String normalizedTitle = trimToNull(title);
        String normalizedTags = trimToNull(tags);
        String normalizedKeyword = trimToNull(keyword);

        wrapper.likeRight(normalizedTitle != null, KnowledgeArticle::getTitle, normalizedTitle)
                .likeRight(normalizedTags != null, KnowledgeArticle::getTags, normalizedTags)
                .likeRight(normalizedKeyword != null && normalizedTitle == null && normalizedTags == null,
                        KnowledgeArticle::getTitle, normalizedKeyword)
                .eq(StringUtils.hasText(category), KnowledgeArticle::getCategory, category)
                .eq(StringUtils.hasText(status), KnowledgeArticle::getStatus, status)
                .orderByDesc(KnowledgeArticle::getId);
        return articleMapper.selectPage(page, wrapper);
    }

    public Map<String, Object> detail(Long id) {
        KnowledgeArticle article = requireArticle(id);
        List<KnowledgeTicketLink> links = linkMapper.selectList(new LambdaQueryWrapper<KnowledgeTicketLink>()
                .eq(KnowledgeTicketLink::getArticleId, id)
                .orderByAsc(KnowledgeTicketLink::getId));
        Map<String, Object> data = new HashMap<>();
        data.put("article", article);
        data.put("links", links);
        data.put("sourceTicket", buildTicketSummary(article.getSourceTicketId(), "source"));
        data.put("linkTickets", buildLinkedTicketSummaries(links));
        data.put("attachments", attachmentMapper.selectList(new LambdaQueryWrapper<TicketAttachment>()
                .eq(TicketAttachment::getArticleId, id)
                .orderByAsc(TicketAttachment::getId)));
        data.put("files", listArticleFiles(id));
        return data;
    }

    private List<Map<String, Object>> buildLinkedTicketSummaries(List<KnowledgeTicketLink> links) {
        if (links == null || links.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> records = new ArrayList<>();
        for (KnowledgeTicketLink link : links) {
            Map<String, Object> summary = buildTicketSummary(link.getTicketId(), link.getLinkType());
            if (summary != null) {
                records.add(summary);
            }
        }
        return records;
    }

    private Map<String, Object> buildTicketSummary(Long ticketId, String linkType) {
        if (ticketId == null) {
            return null;
        }
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            return null;
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", ticket.getId());
        summary.put("ticketNo", ticket.getTicketNo());
        summary.put("title", ticket.getTitle());
        summary.put("category", ticket.getCategory());
        summary.put("priority", ticket.getPriority());
        summary.put("status", ticket.getStatus());
        summary.put("creatorName", ticket.getCreatorName());
        summary.put("handlerName", ticket.getHandlerName());
        summary.put("mergeParentId", ticket.getMergeParentId());
        summary.put("mergeReason", ticket.getMergeReason());
        summary.put("linkType", Objects.requireNonNullElse(linkType, "manual"));
        return summary;
    }

    /**
     * 代码生成/修改时间：2026-07-05。
     * 功能说明：保存知识库草稿或审核驳回后的修订内容，防止待审核和已发布内容被绕过审核直接修改。
     * 入参：KnowledgeSaveDTO。
     * 出参：知识库文章 ID。
     * 异常场景：标题为空、解决步骤为空、当前状态不允许编辑时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long save(KnowledgeSaveDTO dto) {
        dto = requirePayload(dto, "知识库内容不能为空");
        validateText(dto.getTitle(), "知识标题不能为空");
        validateText(dto.getSolutionSteps(), "解决步骤不能为空");

        KnowledgeArticle article = dto.getId() == null ? new KnowledgeArticle() : requireArticle(dto.getId());
        if (article.getId() != null && !List.of(STATUS_DRAFT, STATUS_REJECTED).contains(article.getStatus())) {
            throw new IllegalArgumentException("只有草稿或审核驳回的知识可以编辑");
        }
        article.setTitle(dto.getTitle().trim());
        article.setCategory(defaultText(dto.getCategory(), "general"));
        article.setTags(dto.getTags());
        article.setPhenomenon(dto.getPhenomenon());
        article.setCauseAnalysis(dto.getCauseAnalysis());
        article.setSolutionSteps(dto.getSolutionSteps());
        article.setApplicableScope(dto.getApplicableScope());
        article.setSourceTicketId(dto.getSourceTicketId());
        if (!StringUtils.hasText(article.getStatus())) {
            article.setStatus(STATUS_DRAFT);
        }
        if (article.getUsefulCount() == null) {
            article.setUsefulCount(0);
        }
        if (article.getUselessCount() == null) {
            article.setUselessCount(0);
        }

        if (article.getId() == null) {
            articleMapper.insert(article);
        } else {
            articleMapper.updateById(article);
        }
        linkTickets(article.getId(), dto.getLinkedTicketIds(), "manual");
        linkFiles(article.getId(), dto.getFileIds());
        return article.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long draftFromTicket(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("来源工单不存在");
        }
        if (!StringUtils.hasText(ticket.getSolution())) {
            throw new IllegalArgumentException("工单尚无解决方案，不能生成知识库草稿");
        }

        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(ticket.getTitle());
        article.setCategory(ticket.getCategory());
        article.setTags(ticket.getCategory());
        article.setPhenomenon(ticket.getDescription());
        article.setCauseAnalysis("");
        article.setSolutionSteps(ticket.getSolution());
        article.setApplicableScope("来源工单：" + ticket.getTicketNo());
        article.setStatus(STATUS_DRAFT);
        article.setSourceTicketId(ticket.getId());
        article.setUsefulCount(0);
        article.setUselessCount(0);
        articleMapper.insert(article);
        linkTickets(article.getId(), List.of(ticket.getId()), "source");
        return article.getId();
    }

    /**
     * 代码生成/修改时间：2026-07-05。
     * 功能说明：审核通过知识库文章并发布，保证知识必须先进入待审核状态。
     * 入参：知识库文章 ID。
     * 出参：无。
     * 异常场景：文章不存在或状态不是待审核时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        KnowledgeArticle article = requireArticle(id);
        if (!STATUS_REVIEWING.equals(article.getStatus())) {
            throw new IllegalArgumentException("只有待审核知识可以发布");
        }
        article.setStatus(STATUS_PUBLISHED);
        article.setPublishTime(LocalDateTime.now());
        article.setRemark("");
        articleMapper.updateById(article);
    }

    /**
     * 代码生成/修改时间：2026-07-04。
     * 功能说明：提交知识库文章进入审核状态，发布前由专家或主管复核内容质量。
     * 入参：知识库文章 ID。
     * 出参：无。
     * 异常场景：文章不存在或当前状态不允许提交审核时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitReview(Long id) {
        KnowledgeArticle article = requireArticle(id);
        if (!List.of(STATUS_DRAFT, STATUS_REJECTED).contains(article.getStatus())) {
            throw new IllegalArgumentException("只有草稿或审核驳回的知识可以提交审核");
        }
        article.setStatus(STATUS_REVIEWING);
        articleMapper.updateById(article);
    }

    /**
     * 代码生成/修改时间：2026-07-04。
     * 功能说明：驳回知识库审核，保留文章内容并记录审核意见。
     * 入参：知识库文章 ID、驳回原因。
     * 出参：无。
     * 异常场景：驳回原因为空或文章状态不允许驳回时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectReview(Long id, String reason) {
        validateText(reason, "审核驳回原因不能为空");
        KnowledgeArticle article = requireArticle(id);
        if (!STATUS_REVIEWING.equals(article.getStatus())) {
            throw new IllegalArgumentException("只有待审核知识可以驳回");
        }
        article.setStatus(STATUS_REJECTED);
        article.setRemark(reason.trim());
        articleMapper.updateById(article);
    }

    /**
     * 代码生成/修改时间：2026-07-05。
     * 功能说明：下架已发布知识库文章，保留文章和关联工单记录。
     * 入参：知识库文章 ID。
     * 出参：无。
     * 异常场景：文章不存在或状态不是已发布时抛出 IllegalArgumentException。
     */
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id) {
        KnowledgeArticle article = requireArticle(id);
        if (!STATUS_PUBLISHED.equals(article.getStatus())) {
            throw new IllegalArgumentException("只有已发布知识可以下架");
        }
        article.setStatus(STATUS_WITHDRAWN);
        articleMapper.updateById(article);
    }

    private void linkTickets(Long articleId, List<Long> ticketIds, String linkType) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return;
        }
        for (Long ticketId : ticketIds) {
            if (ticketId == null) {
                continue;
            }
            if (ticketMapper.selectById(ticketId) == null) {
                continue;
            }
            Long existing = linkMapper.selectCount(new LambdaQueryWrapper<KnowledgeTicketLink>()
                    .eq(KnowledgeTicketLink::getArticleId, articleId)
                    .eq(KnowledgeTicketLink::getTicketId, ticketId)
                    .eq(KnowledgeTicketLink::getLinkType, linkType));
            if (existing != null && existing > 0) {
                continue;
            }
            KnowledgeTicketLink link = new KnowledgeTicketLink();
            link.setArticleId(articleId);
            link.setTicketId(ticketId);
            link.setLinkType(linkType);
            linkMapper.insert(link);
        }
    }

    private void linkFiles(Long articleId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        for (Long fileId : fileIds) {
            if (fileId == null) {
                continue;
            }
            // 附件必须先进入 sys_file，再建立知识库关联，保证后续预览和下载可追溯。
            if (sysFileMapper.selectById(fileId) == null) {
                continue;
            }
            Long existing = attachmentMapper.selectCount(new LambdaQueryWrapper<TicketAttachment>()
                    .eq(TicketAttachment::getArticleId, articleId)
                    .eq(TicketAttachment::getFileId, fileId)
                    .eq(TicketAttachment::getBusinessType, "knowledge"));
            if (existing != null && existing > 0) {
                continue;
            }
            TicketAttachment attachment = new TicketAttachment();
            attachment.setBusinessType("knowledge");
            attachment.setArticleId(articleId);
            attachment.setFileId(fileId);
            attachment.setVisibleScope("public");
            attachment.setUploadUserId(userId);
            attachmentMapper.insert(attachment);
        }
    }

    private List<SysFile> listArticleFiles(Long articleId) {
        List<TicketAttachment> attachments = attachmentMapper.selectList(new LambdaQueryWrapper<TicketAttachment>()
                .eq(TicketAttachment::getArticleId, articleId)
                .isNotNull(TicketAttachment::getFileId)
                .orderByAsc(TicketAttachment::getId));
        List<Long> fileIds = attachments.stream()
                .map(TicketAttachment::getFileId)
                .distinct()
                .toList();
        if (fileIds.isEmpty()) {
            return List.of();
        }
        return sysFileMapper.selectBatchIds(fileIds);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private KnowledgeArticle requireArticle(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("知识库文章不能为空");
        }
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new IllegalArgumentException("知识库文章不存在");
        }
        return article;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private void validateText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private <T> T requirePayload(T payload, String message) {
        if (payload == null) {
            throw new IllegalArgumentException(message);
        }
        return payload;
    }
}
