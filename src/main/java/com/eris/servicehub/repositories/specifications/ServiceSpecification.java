package com.eris.servicehub.repositories.specifications;

import com.eris.servicehub.entities.Category;
import com.eris.servicehub.entities.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.UUID;

public class ServiceSpecification {

    public static Specification<Service> hasText(String query) {
        return (root, cq, cb) -> {
            if (!StringUtils.hasText(query)) {
                return cb.conjunction();
            }
            String likePattern = "%" + query.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), likePattern),
                    cb.like(cb.lower(root.get("description")), likePattern)
            );
        };
    }

    public static Specification<Service> byCategoryId(UUID categoryId) {
        return (root, cq, cb) -> {
            if (categoryId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }
}