package com.rbac.base.modules.ticket.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_WITHDRAWN = "withdrawn";

    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeTicketLinkMapper linkMapper;
    private final TicketMapper ticketMapper;
    private final TicketAttachmentMapper attachmentMapper;

    public KnowledgeService(KnowledgeArticleMapper articleMapper,
                            KnowledgeTicketLinkMapper linkMapper,
                            TicketMapper ticketMapper,
                            TicketAttachmentMapper attachmentMapper) {
        this.articleMapper = articleMapper;
        this.linkMapper = linkMapper;
        this.ticketMapper = ticketMapper;
        this.attachmentMapper = attachmentMapper;
    }

    public Page<KnowledgeArticle> page(Integer pageNum, Integer pageSize, String keyword, String category, String status) {
        Page<KnowledgeArticle> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(KnowledgeArticle::getTitle, keyword)
                    .or()
                    .like(KnowledgeArticle::getPhenomenon, keyword)
                    .or()
                    .like(KnowledgeArticle::getSolutionSteps, keyword)
                    .or()
                    .like(KnowledgeArticle::getTags, keyword));
        }
        wrapper.eq(StringUtils.hasText(category), KnowledgeArticle::getCategory, category)
                .eq(StringUtils.hasText(status), KnowledgeArticle::getStatus, status)
                .orderByDesc(KnowledgeArticle::getId);
        return articleMapper.selectPage(page, wrapper);
    }

    public Map<String, Object> detail(Long id) {
        KnowledgeArticle article = requireArticle(id);
        Map<String, Object> data = new HashMap<>();
        data.put("article", article);
        data.put("links", linkMapper.selectList(new LambdaQueryWrapper<KnowledgeTicketLink>()
                .eq(KnowledgeTicketLink::getArticleId, id)
                .orderByAsc(KnowledgeTicketLink::getId)));
        data.put("attachments", attachmentMapper.selectList(new LambdaQueryWrapper<TicketAttachment>()
                .eq(TicketAttachment::getArticleId, id)
                .orderByAsc(TicketAttachment::getId)));
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long save(KnowledgeSaveDTO dto) {
        validateText(dto.getTitle(), "知识标题不能为空");
        validateText(dto.getSolutionSteps(), "解决步骤不能为空");

        KnowledgeArticle article = dto.getId() == null ? new KnowledgeArticle() : requireArticle(dto.getId());
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

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        KnowledgeArticle article = requireArticle(id);
        article.setStatus(STATUS_PUBLISHED);
        article.setPublishTime(LocalDateTime.now());
        articleMapper.updateById(article);
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id) {
        KnowledgeArticle article = requireArticle(id);
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
            TicketAttachment attachment = new TicketAttachment();
            attachment.setBusinessType("knowledge");
            attachment.setArticleId(articleId);
            attachment.setFileId(fileId);
            attachment.setVisibleScope("public");
            attachment.setUploadUserId(userId);
            attachmentMapper.insert(attachment);
        }
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
}
