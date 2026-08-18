package com.pse.search;

import com.pse.model.dto.SearchRequest;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;

@Component
public class SearchQueryBuilder {

    public CriteriaQuery build(SearchRequest request) {
        Criteria criteria = new Criteria();
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            Criteria nameCriteria = new Criteria("name").matches(request.getQuery());
            Criteria brandCriteria = new Criteria("brand").matches(request.getQuery());
            criteria = criteria.or(nameCriteria).or(brandCriteria);
        }
        if (request.getCategory() != null) {
            criteria = criteria.and(new Criteria("category").is(request.getCategory().name()));
        }
        if (request.getBrand() != null) {
            criteria = criteria.and(new Criteria("brand").is(request.getBrand()));
        }
        if (request.getMinPrice() != null) {
            criteria = criteria.and(new Criteria("price").greaterThanEqual(request.getMinPrice()));
        }
        if (request.getMaxPrice() != null) {
            criteria = criteria.and(new Criteria("price").lessThanEqual(request.getMaxPrice()));
        }
        return new CriteriaQuery(criteria)
                .setPageable(request.toPageable());
    }
}
