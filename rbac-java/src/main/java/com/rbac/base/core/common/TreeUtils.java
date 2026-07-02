package com.rbac.base.core.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 树形结构构建工具类
 * <p>
 * 提供通用的树形结构构建功能，支持将扁平列表转换为树形结构。
 * 适用于任何需要构建树形结构的场景，如菜单树、组织架构树、分类树等。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * List<MenuItem> tree = TreeUtils.buildTree(
 *     menuList,
 *     MenuItem::getId,
 *     MenuItem::getParentId,
 *     0L,
 *     item -> new MenuItem(item),
 *     MenuItem::getChildren,
 *     MenuItem::setChildren
 * );
 * }</pre>
 * </p>
 *
 * @author RBAC Team
 * @since 1.0.0
 */
public final class TreeUtils {

    private TreeUtils() {
    }

    /**
     * 将扁平列表构建为树形结构
     * <p>
     * 该方法接收一个扁平的数据列表，根据父子关系将其构建为树形结构。
     * 支持自定义节点 ID、父节点 ID 的获取方式，以及子节点的存储方式。
     * </p>
     * <p>
     * 构建逻辑：
     * <ol>
     *   <li>将所有节点复制到 Map 中（以节点 ID 为 key）</li>
     *   <li>遍历所有节点，根据 parentId 将子节点连接到父节点</li>
     *   <li>将没有父节点或父节点不存在的节点作为根节点返回</li>
     * </ol>
     * </p>
     *
     * @param <T>            节点类型
     * @param <K>            节点 ID 类型
     * @param items          原始数据列表，包含所有待构建的节点
     * @param idGetter       获取节点唯一标识的函数，如 Node::getId
     * @param parentGetter   获取节点父级标识的函数，如 Node::getParentId
     * @param rootValue      根节点的父级标识值，当 parentId 等于此值时视为根节点
     * @param copier         节点复制函数，用于创建节点的深拷贝，避免修改原始数据
     * @param childrenGetter 获取节点子节点列表的函数，如 Node::getChildren
     * @param childrenSetter 设置节点子节点列表的函数，如 Node::setChildren
     * @return 构建完成的树形结构根节点列表
     * @throws IllegalArgumentException 当 items 为 null 时抛出
     * @throws NullPointerException     当必要参数为 null 时抛出
     */
    public static <T, K> List<T> buildTree(
            List<T> items,
            Function<T, K> idGetter,
            Function<T, K> parentGetter,
            K rootValue,
            Function<T, T> copier,
            Function<T, List<T>> childrenGetter,
            BiConsumer<T, List<T>> childrenSetter
    ) {
        Map<K, T> nodeMap = new LinkedHashMap<>();
        for (T item : items) {
            T node = copier.apply(item);
            List<T> children = childrenGetter.apply(node);
            if (children == null) {
                childrenSetter.accept(node, new ArrayList<>());
            } else {
                children.clear();
            }
            nodeMap.put(idGetter.apply(node), node);
        }

        List<T> tree = new ArrayList<>();
        for (T node : nodeMap.values()) {
            K parentId = parentGetter.apply(node);
            if (!Objects.equals(parentId, rootValue) && nodeMap.containsKey(parentId)) {
                childrenGetter.apply(nodeMap.get(parentId)).add(node);
                continue;
            }
            tree.add(node);
        }
        return tree;
    }
}
