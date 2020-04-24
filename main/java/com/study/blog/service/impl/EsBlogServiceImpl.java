package com.study.blog.service.impl;

import com.study.blog.entity.EsBlog;
import com.study.blog.entity.User;
import com.study.blog.repository.es2search.EsBlogRepository;
import com.study.blog.service.EsBlogService;
import com.study.blog.service.UserService;
import com.study.blog.vo.TagVO;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * @author 10652
 */
@Service
public class EsBlogServiceImpl implements EsBlogService {

    /**
     * 分页常量：大小为5
     */
    private static final Pageable TOP_5_PAGABLE = PageRequest.of(0, 5);
    /**
     * 空字符串常量
     */
    private static final String EMPTY_KEYWORD = "";
    private final EsBlogRepository esBlogRepository;
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final UserService userService;

    @Autowired
    public EsBlogServiceImpl(EsBlogRepository esBlogRepository, ElasticsearchTemplate elasticsearchTemplate,
                             UserService userService) {
        this.esBlogRepository = esBlogRepository;
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.userService = userService;
    }

    /**
     * @param id id
     */
    @Override
    public void removeEsBlog(String id) {
        esBlogRepository.deleteById(id);
    }

    /**
     * 更新保存 ES Blog
     *
     * @param esBlog EsBlog
     * @return EsBlog
     */
    @Override
    public EsBlog updateEsBlog(EsBlog esBlog) {
        return esBlogRepository.save(esBlog);
    }

    /**
     * 更具博客id获取博客：要求 blogId 字段的 index=true：即建立（倒排）索引
     *
     * @param blogId 博客id
     * @return EsBlog
     */
    @Override
    public EsBlog getEsBlogByBlogId(Long blogId) {
        return esBlogRepository.findByBlogId(blogId);
    }

    /**
     * 列出所有 EsBlog
     *
     * @param pageable 分页
     * @return EsBlog列表
     */
    @Override
    public Page<EsBlog> listEsBlog(Pageable pageable) {
        return esBlogRepository.findAll(pageable);
    }

    /**
     * 最新排序 EsBlog
     *
     * @param keyword  关键字
     * @param pageable 分页
     * @return EsBlog 列表
     */
    @Override
    public Page<EsBlog> listNewestEsBlog(String keyword, Pageable pageable) {

        Page<EsBlog> blogs;

        /*
         * 判断搜索条件是否已经设置排序方式：若未设置，则默认按照最新排序
         *
         * 注意：Pageable 中的 Sort 参数永远不为 null ，该 Sort 设置有特有的表示 未排序的标识：Sort.unsorted()
         */
        if (Objects.equals(pageable.getSort(), Sort.unsorted())) {
            Sort sort = new Sort(Sort.Direction.DESC, "createTime");
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        }

        blogs = esBlogRepository.findDistinctByTitleContainingOrSummaryContainingOrContentContainingOrTagsContaining
                (keyword, keyword, keyword, keyword, pageable);
        return blogs;
    }

    /**
     * 最热排序：按照 阅读量、评论量、点赞量 作为最热排序标准，相同热度的文章按照最新排序
     *
     * @param keyword  关键字
     * @param pageable 分页
     * @return 博客列表
     */
    @Override
    public Page<EsBlog> listHotestEsBlog(String keyword, Pageable pageable) {
        Page<EsBlog> blogs;

        if (Objects.equals(pageable.getSort(), Sort.unsorted())) {
            Sort sort = new Sort(Sort.Direction.DESC, "readCount", "commentCount", "likeCount", "createTime");
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        }

        blogs = esBlogRepository.findDistinctByTitleContainingOrSummaryContainingOrContentContainingOrTagsContaining
                (keyword, keyword, keyword, keyword, pageable);
        return blogs;
    }

    /**
     * 最新 前5
     *
     * @return EsBlog列表
     */
    @Override
    public List<EsBlog> listTop5NewestEsBlog() {
        Page<EsBlog> page = this.listNewestEsBlog(EMPTY_KEYWORD, TOP_5_PAGABLE);
        return page.getContent();
    }

    /**
     * 最热 前5
     *
     * @return EsBlog列表
     */
    @Override
    public List<EsBlog> listTop5HotestEsBlog() {
        Page<EsBlog> page = this.listHotestEsBlog(EMPTY_KEYWORD, TOP_5_PAGABLE);
        return page.getContent();
    }

    /**
     * 最热 前30个标签
     *
     * @return tags
     */
    @Override
    public List<TagVO> listTop30Tag() {

        List<TagVO> tags = new ArrayList<>(1);

        // 1、构造查询条件
    /*
        ES 原生提供的功能：热时统计、聚合 等功能
        使用的是 Elasticsearch 中原生的查询方式
        构造了一个新的查询条件：通过聚合，将tags作为查询条件，查询出前30个最热标签
     */
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchAllQuery())
                .withSearchType(SearchType.QUERY_THEN_FETCH)
                .withIndices("blog")
                .withTypes("blog")
                .addAggregation(
                        // 注意：此处已有变动
                    /*
                        AggregationBuilders.terms()
                        而不是 terms()

                        order(BucketOrder.count(false)).size(30)
                        而不是 order(Terms.Order.count(false)).size(30)
                    */
                        AggregationBuilders.terms("tags").field("tags").order(BucketOrder.count(false)).size(30).shardSize(40)
                ).build();

        // 2、执行上述查询条件，得到聚合结果
    /*
        由 函数表达式
         new ResultsExtractor<Aggregations>() {
            @Override
            Public Aggregations extract(SearchResponse searchResponse) {
                return searchResponse.getAggregations()
            }
        }

        转换为 lambda 表达式
            searchResponse -> searchResponse.getAggregations()
        最后转换为下述 方法引用
            SearchResponse::getAggregations
     */
        Aggregations aggregations = elasticsearchTemplate.query(searchQuery, SearchResponse::getAggregations);

        // 3、从聚合结果中获取的聚合子类 tags ，该聚合子类设置为map集合,使得map的value就是桶Bucket，我们要获得Bucket
        StringTerms modelTerms = (StringTerms) aggregations.asMap().get("tags");
        for (StringTerms.Bucket tagsBucket : modelTerms.getBuckets()) {
            tags.add(new TagVO(tagsBucket.getKey().toString(), tagsBucket.getDocCount()));
        }

        return tags;
    }

    /**
     * 最热的前12名用户，查询方式与上述一致！
     *
     * @return 用户列表
     */
    @Override
    public List<User> listTop12User() {
        List<String> usernameList = new ArrayList<>(1);

        // 1、创建查询条件
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchAllQuery())
                .withSearchType(SearchType.QUERY_THEN_FETCH)
                .withIndices("blog")
                .withTypes("blog")
                .addAggregation(
                        AggregationBuilders.terms("users").field("username").order(BucketOrder.count(false)).size(12)
                ).build();

        // 2、执行查询：获取到聚合结果
        Aggregations aggregations = elasticsearchTemplate.query(searchQuery, SearchResponse::getAggregations);

        // 3、从聚合结果中获取聚合子类
        StringTerms userTerms = (StringTerms) aggregations.asMap().get("users");
        for (StringTerms.Bucket userBucket : userTerms.getBuckets()) {
            String username = userBucket.getKeyAsString();
            usernameList.add(username);
        }
        if (usernameList.size() <= 0) {
            return new ArrayList<>(1);
        }
        return userService.listUserByName(usernameList);
    }

}
